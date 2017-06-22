/*! Datatables altEditor 1.0
 */
/**
 * @summary     altEditor
 * @description Lightweight editor for DataTables
 * @version     1.0
 * @file        dataTables.editor.lite.js
 * @author      kingkode (www.kingkode.com)
 * @contact     www.kingkode.com/contact
 * @copyright   Copyright 2016 Kingkode
 *
 * This source file is free software, available under the following license:
 *   MIT license - http://datatables.net/license/mit
 *
 * This source file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the license files for details.
 *
 * For details please refer to: http://www.kingkode.com
 */
(function(factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD
        define(['jquery', 'datatables.net'], function($) {
            return factory($, window, document);
        });
    } else if (typeof exports === 'object') {
        // CommonJS
        module.exports = function(root, $) {
            if (!root) {
                root = window;
            }

            if (!$ || !$.fn.dataTable) {
                $ = require('datatables.net')(root, $).$;
            }

            return factory($, root, root.document);
        };
    } else {
        // Browser
        factory(jQuery, window, document);
    }
}(function($, window, document, undefined) {
    'use strict';
    var DataTable = $.fn.dataTable;


    var _instance = 0;

    /**
     * altEditor provides modal editing of records for Datatables
     *
     * @class altEditor
     * @constructor
     * @param {object} oTD DataTables settings object
     * @param {object} oConfig Configuration object for altEditor
     */
    var altEditor = function(dt, opts) {
        if (!DataTable.versionCheck || !DataTable.versionCheck('1.10.8')) {
            throw ("Warning: altEditor requires DataTables 1.10.8 or greater");
        }

        // User and defaults configuration object
        this.c = $.extend(true, {},
            DataTable.defaults.altEditor,
            altEditor.defaults,
            opts
        );

        /**
         * @namespace Settings object which contains customisable information for altEditor instance
         */
        this.s = {
            /** @type {DataTable.Api} DataTables' API instance */
            dt: new DataTable.Api(dt),

            /** @type {String} Unique namespace for events attached to the document */
            namespace: '.altEditor' + (_instance++)
        };


        /**
         * @namespace Common and useful DOM elements for the class instance
         */
        this.dom = {
            /** @type {jQuery} altEditor handle */
            modal: $('<div class="dt-altEditor-handle"/>'),
        };


        /* Constructor logic */
        this._constructor();
    }



    $.extend(altEditor.prototype, {
        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
         * Constructor
         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

        /**
         * Initialise the RowReorder instance
         *
         * @private
         */
        _constructor: function() {
            // console.log('altEditor Enabled')
            var that = this;
            var dt = this.s.dt;

            this._setup();

            dt.on('destroy.altEditor', function() {
                dt.off('.altEditor');
                $(dt.table().body()).off(that.s.namespace);
                $(document.body).off(that.s.namespace);
            });
        },

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
         * Private methods
         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

        /**
         * Setup dom and bind button actions
         *
         * @private
         */
        _setup: function() {
            // console.log('Setup');

            var that = this;
            var dt = this.s.dt;

            // add modal
            $('body').append('\
            <div class="modal fade" id="altEditor-modal" tabindex="-1" role="dialog">\
              <div class="modal-dialog">\
                <div class="modal-content">\
                  <div class="modal-header">\
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
                    <h4 class="modal-title"></h4>\
                  </div>\
                  <div class="modal-body">\
                    <p></p>\
                  </div>\
                  <div class="modal-footer">\
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>\
                    <button type="button" class="btn btn-primary">Save changes</button>\
                  </div>\
                </div>\
              </div>\
            </div>');


            // add Edit Button
            if (this.s.dt.button('edit:name')) {
                this.s.dt.button('edit:name').action(function(e, dt, node, config) {
                    var rows = dt.rows({
                        selected: true
                    }).count();

                    that._openEditModal();
                });

                $(document).on('click', '#editRowBtn', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    that._editRowData();
                });

            }

            // add Delete Button
            if (this.s.dt.button('delete:name')) {
                this.s.dt.button('delete:name').action(function(e, dt, node, config) {
                    var rows = dt.rows({
                        selected: true
                    }).count();

                    that._openDeleteModal();
                });

                $(document).on('click', '#deleteRowBtn', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    that._deleteRow();
                });
            }

            // add Add Button
            if (this.s.dt.button('add:name')) {
                this.s.dt.button('add:name').action(function(e, dt, node, config) {
                    var rows = dt.rows({
                        selected: true
                    }).count();

                    that._openAddModal();
                });

                $(document).on('click', '#addRowBtn', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    that._addRowData();
                });
            }

        },

        /**
         * Emit an event on the DataTable for listeners
         *
         * @param  {string} name Event name
         * @param  {array} args Event arguments
         * @private
         */
        _emitEvent: function(name, args) {
            this.s.dt.iterator('table', function(ctx, i) {
                $(ctx.nTable).triggerHandler(name + '.dt', args);
            });
        },

        /**
         * Open Edit Modal for selected row
         *
         * @private
         */
        _openEditModal: function() {
            var that = this;
            var dt = this.s.dt;

            var columnDefs = [];

            for (var i = 0; i < dt.context[0].aoColumns.length; i++) {
                columnDefs.push({
                    title: dt.context[0].aoColumns[i].sTitle,
                    dataType: dt.context[0].aoColumns[i].dataType,
                    isPrimary: dt.context[0].aoColumns[i].isPrimary,
                })
            }

            var adata = dt.rows({
                selected: true
            });


            var data = "";

            data += "<form class='form-horizontal' name='altEditor-form' role='form'>";

            for (var j in columnDefs) {
                var cellData = adata.data()[0][j];
                var inputSectionHTML = "<div class='form-group'><label for='__INPUT_NAME__' class='col-sm-4 control-label'>__INPUT_NAME__</label>__INPUT_HTML__</div>";

                var inputHTML = "<div class='col-sm-7'><input data-type='__INPUT_DATA_TYPE__' __INPUT_READ_ONLY_ATTRIBUTE__ type='__INPUT_TYPE__'  id='__INPUT_NAME__' name='__INPUT_NAME__' placeholder='__INPUT_NAME__' style='overflow:hidden'  class='form-control' value='__INPUT_VALUE__'></div>";

                var option1Checked = "";
                var option2Checked = "checked";
                if (cellData.dataType == "boolean") {
                    if(JSON.parse(cellData.value)) {
                        option1Checked = "checked";
                        option2Checked = "";
                    }
                    inputHTML = "<div class='col-sm-7'><div class='checkbox'><label><label class='radio-inline'><input data-type='__INPUT_DATA_TYPE__' __OPTION_1_CHECKED__ type='radio' name='__INPUT_NAME__' id='__INPUT_NAME__' value='1'>true</label><label class='radio-inline'><input data-type='__INPUT_DATA_TYPE__' __OPTION_2_CHECKED__ type='radio' name='__INPUT_NAME__' id='__INPUT_NAME__' value='0'>false</label></div></div>"
                }
                //set input type
                var inputType = "text";
                var inputReadOnlyAttribute = "";
                switch (cellData.dataType) {
                    case 'integer':
                        inputType = "number";
                        break;
                    case 'real':
                        inputType = "number";
                        break;
                    case 'boolean':
                        inputType = "checkbox";
                        break;
                    case 'long':
                        inputType = "number";
                        break;
                    case 'float':
                        inputType = "number";
                        break;
                    case 'text':
                        inputType = "text";
                        break;
                    case 'string_set':
                        inputType = "text";
                        break;
                }
                //set input to read-only if it is a primary key
                if (columnDefs[j].isPrimary) {
                    inputReadOnlyAttribute = "readonly"
                }

                //append input html
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_HTML__/g, inputHTML);
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_READ_ONLY_ATTRIBUTE__/g, inputReadOnlyAttribute);
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_TYPE__/g, inputType);
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_DATA_TYPE__/g, cellData.dataType);
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_VALUE__/g, cellData.value);
                inputSectionHTML = inputSectionHTML.replace(/__INPUT_NAME__/g, columnDefs[j].title);
                inputSectionHTML = inputSectionHTML.replace(/__OPTION_1_CHECKED__/g, option1Checked);
                inputSectionHTML = inputSectionHTML.replace(/__OPTION_2_CHECKED__/g, option2Checked);
                data += inputSectionHTML;
            }
            data += "</form>";


            $('#altEditor-modal').on('show.bs.modal', function() {
                $('#altEditor-modal').find('.modal-title').html('Edit Record');
                $('#altEditor-modal').find('.modal-body').html('<pre>' + data + '</pre>');
                $('#altEditor-modal').find('.modal-footer').html("<button type='button' data-content='remove' class='btn btn-default' data-dismiss='modal'>Close</button>\
               <button type='button' data-content='remove' class='btn btn-primary' id='editRowBtn'>Save Changes</button>");
            });

            $('#altEditor-modal').modal('show');
            $('#altEditor-modal input[0]').focus();

        },

        _editRowData: function() {
            var that = this;
            var dt = this.s.dt;

            var data = [];

            $('form[name="altEditor-form"] input').each(function(i) {
                var addToList = true;
                var value = $(this).val();
                if($(this).attr('type') == "radio" && $(this).prop('checked') == false) {
                    addToList = false;
                }
                value = $(this).attr('type') == "radio" ? $(this).val() == "1" : value;
                if (addToList){
                    data.push({
                        "value": value,
                        "dataType": $(this).attr('data-type')
                    });
                }
            });
            var editButtonCurrentText = $("#editRowBtn").text();
            $("#editRowBtn").addClass('disabled');
            $("#editRowBtn").text("Saving..");
            that._emitEvent("update-row", [
                JSON.stringify(data),
                function(isUpdated) {


                    //set error message and other properties based on whether update is successfull or not
                    var alertAdditionClasses = "alert-success";
                    var alertMessage = "This record has been updated";
                    var alertHeading = "Success";
                    if (!isUpdated) {
                        alertAdditionClasses = "alert-danger";
                        alertMessage = "Error occurred while updating this record";
                        alertHeading = "Error";
                    }

                    //create alert element html and append it to modal
                    var messageHTML = '\
                        <div class="alert __ALERT_ADDITION_CLASSES__" role="alert">\
                            <strong>__ALERT_HEADING__!</strong>\
                            __ALERT_MESSAGE__.\
                        </div>\
                    ';
                    messageHTML = messageHTML.replace(/__ALERT_ADDITION_CLASSES__/g, alertAdditionClasses);
                    messageHTML = messageHTML.replace(/__ALERT_HEADING__/g, alertHeading);
                    messageHTML = messageHTML.replace(/__ALERT_MESSAGE__/g, alertMessage);
                    $('#altEditor-modal .modal-body').append(messageHTML);

                    //update datatable, if update is successfull
                    if (isUpdated) {
                        dt.row({
                            selected: true
                        }).data(data);
                        //remove existing alert elements
                        $('#altEditor-modal').modal('hide');
                    }
                    $("#editRowBtn").removeClass('disabled');
                    $("#editRowBtn").text(editButtonCurrentText);
                }
            ]);
        },


        /**
         * Open Delete Modal for selected row
         *
         * @private
         */
        _openDeleteModal: function() {
            var that = this;
            var dt = this.s.dt;

            var columnDefs = [];

            for (var i = 0; i < dt.context[0].aoColumns.length; i++) {
                columnDefs.push({
                    title: dt.context[0].aoColumns[i].sTitle
                })
            }

            var adata = dt.rows({
                selected: true
            });

            var data = "";

            data += "<form name='altEditor-form' role='form'>";
            for (var i in columnDefs) {
                var cellData = adata.data()[0][i];

                var inputType = "text";
                switch (cellData.dataType) {
                    case 'integer':
                        inputType = "number";
                        break;
                    case 'real':
                        inputType = "number";
                        break;
                    case 'boolean':
                        inputType = "checkbox";
                        break;
                    case 'long':
                        inputType = "number";
                        break;
                    case 'float':
                        inputType = "number";
                        break;
                    case 'text':
                        inputType = "text";
                        break;
                    case 'string_set':
                        inputType = "text";
                        break;
                }

                data += "<div class='form-group'><label for='" + columnDefs[i].title + "'>" + columnDefs[i].title + " : </label><input  type='hidden' data-type='" + inputType + "'  id='" + columnDefs[i].title + "' name='" + columnDefs[i].title + "' placeholder='" + columnDefs[i].title + "' style='overflow:hidden'  class='form-control' value='" + cellData.value + "' >" + cellData.value + "</input></div>";
            }
            data += "</form>";

            $('#altEditor-modal').on('show.bs.modal', function() {
                $('#altEditor-modal').find('.modal-title').html('Delete Record');
                $('#altEditor-modal').find('.modal-body').html('<pre>' + data + '</pre>');
                $('#altEditor-modal').find('.modal-footer').html("<button type='button' data-content='remove' class='btn btn-default' data-dismiss='modal'>Close</button>\
               <button type='button' data-content='remove' class='btn btn-danger' id='deleteRowBtn'>Delete</button>");
            });

            $('#altEditor-modal').modal('show');
            $('#altEditor-modal input[0]').focus();

        },

        _deleteRow: function() {
            var that = this;
            var dt = this.s.dt;

            var data = [];

            $('form[name="altEditor-form"] input').each(function(i) {
                var addToList = true;
                var value = $(this).val();
                value = $(this).val();

                console.log("Value : " + value);
                if (addToList){
                    data.push({
                        "value": value,
                        "dataType": $(this).attr('data-type')
                    });
                }
            });

            $('#altEditor-modal .modal-body .alert').remove();

            var message = '<div class="alert alert-success" role="alert">\
           <strong>Success!</strong> This record has been deleted.\
         </div>';

            $('#altEditor-modal .modal-body').append(message);


            that._emitEvent("delete-row", [
                            JSON.stringify(data),
                            function(isDeleted) {
                               if (isDeleted) {
                                    dt.row({
                                        selected: true
                                    }).remove();

                                    dt.draw();
                               }

                                //remove existing alert elements
                                $('#altEditor-modal').modal('hide');
                            }
                        ]);



        },


        /**
         * Open Add Modal for selected row
         *
         * @private
         */
        _openAddModal: function() {
            var that = this;
            var dt = this.s.dt;

            var columnDefs = [];

            for (var i = 0; i < dt.context[0].aoColumns.length; i++) {
                columnDefs.push({
                    title: dt.context[0].aoColumns[i].sTitle,
                    dataType: dt.context[0].aoColumns[i].sType,
                    isPrimary: dt.context[0].aoColumns[i].isPrimary,
                    value : "",
                })
            }


           var data = "";

           data += "<form class='form-horizontal' name='altEditor-form' role='form'>";

           for (var j in columnDefs) {
               var inputSectionHTML = "<div class='form-group'><label for='__INPUT_NAME__' class='col-sm-4 control-label'>__INPUT_NAME__</label>__INPUT_HTML__</div>";

               var inputHTML = "<div class='col-sm-7'><input data-type='__INPUT_DATA_TYPE__' __INPUT_READ_ONLY_ATTRIBUTE__ type='__INPUT_TYPE__'  id='__INPUT_NAME__' name='__INPUT_NAME__' placeholder='__INPUT_NAME__' style='overflow:hidden'  class='form-control' value='__INPUT_VALUE__'></div>";

               var option1Checked = "";
               var option2Checked = "checked";
               if (columnDefs[j].dataType == "boolean") {
                   if(JSON.parse(columnDefs[j].value)) {
                       option1Checked = "checked";
                       option2Checked = "";
                   }
                   inputHTML = "<div class='col-sm-7'><div class='checkbox'><label><label class='radio-inline'><input data-type='__INPUT_DATA_TYPE__' __OPTION_1_CHECKED__ type='radio' name='__INPUT_NAME__' id='__INPUT_NAME__' value='1'>true</label><label class='radio-inline'><input data-type='__INPUT_DATA_TYPE__' __OPTION_2_CHECKED__ type='radio' name='__INPUT_NAME__' id='__INPUT_NAME__' value='0'>false</label></div></div>"
               }
               //set input type
               var inputType = "text";
               var inputReadOnlyAttribute = "";
               switch (columnDefs[j].dataType) {
                   case 'num':
                       inputType = "number";
                       break;
                   case 'string':
                       inputType = "text";
                       break;
               }
               //set input to read-only if it is a primary key
//               if (columnDefs[j].isPrimary) {
//                   inputReadOnlyAttribute = "readonly"
//               }

               //append input html
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_HTML__/g, inputHTML);
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_READ_ONLY_ATTRIBUTE__/g, inputReadOnlyAttribute);
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_TYPE__/g, inputType);
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_DATA_TYPE__/g, columnDefs[j].dataType);
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_VALUE__/g, columnDefs[j].value);
               inputSectionHTML = inputSectionHTML.replace(/__INPUT_NAME__/g, columnDefs[j].title);
               inputSectionHTML = inputSectionHTML.replace(/__OPTION_1_CHECKED__/g, option1Checked);
               inputSectionHTML = inputSectionHTML.replace(/__OPTION_2_CHECKED__/g, option2Checked);
               data += inputSectionHTML;
           }
           data += "</form>";


            $('#altEditor-modal').on('show.bs.modal', function() {
                $('#altEditor-modal').find('.modal-title').html('Add Record');
                $('#altEditor-modal').find('.modal-body').html('<pre>' + data + '</pre>');
                $('#altEditor-modal').find('.modal-footer').html("<button type='button' data-content='remove' class='btn btn-default' data-dismiss='modal'>Close</button>\
               <button type='button' data-content='remove' class='btn btn-primary' id='addRowBtn'>Add Record</button>");
            });

            $('#altEditor-modal').modal('show');
            $('#altEditor-modal input[0]').focus();
        },

        _addRowData: function() {
            console.log('add row')
            var that = this;
            var dt = this.s.dt;

            var data = [];

            $('form[name="altEditor-form"] input').each(function(i) {
                var addToList = true;
                var value = $(this).val();
                if($(this).attr('type') == "radio" && $(this).prop('checked') == false) {
                    addToList = false;
                }
                value = $(this).attr('type') == "radio" ? $(this).val() == "1" : value;
                if (addToList){
                    data.push({
                        "value": value,
                        "dataType": $(this).attr('data-type')
                    });
                }
            });
            var editButtonCurrentText = $("#editRowBtn").text();
            $("#addRowBtn").addClass('disabled');
            $("#addRowBtn").text("Saving..");

            console.log(JSON.stringify(data));

            that._emitEvent("add-row", [
                JSON.stringify(data),
                function(isAdded) {


                    //set error message and other properties based on whether update is successfull or not
                    var alertAdditionClasses = "alert-success";
                    var alertMessage = "This record has been added";
                    var alertHeading = "Success";
                    if (!isAdded) {
                        alertAdditionClasses = "alert-danger";
                        alertMessage = "Error occurred while adding this record";
                        alertHeading = "Error";
                    }

                    //create alert element html and append it to modal
                    var messageHTML = '\
                        <div class="alert __ALERT_ADDITION_CLASSES__" role="alert">\
                            <strong>__ALERT_HEADING__!</strong>\
                            __ALERT_MESSAGE__.\
                        </div>\
                    ';
                    messageHTML = messageHTML.replace(/__ALERT_ADDITION_CLASSES__/g, alertAdditionClasses);
                    messageHTML = messageHTML.replace(/__ALERT_HEADING__/g, alertHeading);
                    messageHTML = messageHTML.replace(/__ALERT_MESSAGE__/g, alertMessage);
                    $('#altEditor-modal .modal-body').append(messageHTML);

                    //update datatable, if update is successfull
                    if (isAdded) {
                        dt.row().data(data);
                        //remove existing alert elements
                        $('#altEditor-modal').modal('hide');
                    }
                    $("#addRowBtn").removeClass('disabled');
                    $("#addRowBtn").text(editButtonCurrentText);
                }
            ]);
        },

        _getExecutionLocationFolder: function() {
            var fileName = "dataTables.altEditor.js";
            var scriptList = $("script[src]");
            var jsFileObject = $.grep(scriptList, function(el) {

                if (el.src.indexOf(fileName) !== -1) {
                    return el;
                }
            });
            var jsFilePath = jsFileObject[0].src;
            var jsFileDirectory = jsFilePath.substring(0, jsFilePath.lastIndexOf("/") + 1);
            return jsFileDirectory;
        }
    });



    /**
     * altEditor version
     *
     * @static
     * @type      String
     */
    altEditor.version = '1.0';


    /**
     * altEditor defaults
     *
     * @namespace
     */
    altEditor.defaults = {
        /** @type {Boolean} Ask user what they want to do, even for a single option */
        alwaysAsk: false,

        /** @type {string|null} What will trigger a focus */
        focus: null, // focus, click, hover

        /** @type {column-selector} Columns to provide auto fill for */
        columns: '', // all

        /** @type {boolean|null} Update the cells after a drag */
        update: null, // false is editor given, true otherwise

        /** @type {DataTable.Editor} Editor instance for automatic submission */
        editor: null
    };


    /**
     * Classes used by altEditor that are configurable
     *
     * @namespace
     */
    altEditor.classes = {
        /** @type {String} Class used by the selection button */
        btn: 'btn'
    };


    // Attach a listener to the document which listens for DataTables initialisation
    // events so we can automatically initialise
    $(document).on('preInit.dt.altEditor', function(e, settings, json) {
        if (e.namespace !== 'dt') {
            return;
        }

        var init = settings.oInit.altEditor;
        var defaults = DataTable.defaults.altEditor;

        if (init || defaults) {
            var opts = $.extend({}, init, defaults);

            if (init !== false) {
                new altEditor(settings, opts);
            }
        }
    });


    // Alias for access
    DataTable.altEditor = altEditor;

    return altEditor;
}));