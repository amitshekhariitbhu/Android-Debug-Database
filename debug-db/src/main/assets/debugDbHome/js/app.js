$( document ).ready(function() {
    getDBList();
    $("#query").keypress(function(e){
        if(e.which == 13) {
            queryFunction();
        }
    });
    //update currently selected database
    $( document ).on( "click", "#db-list .list-group-item", function() {
        $("#db-list .list-group-item").each(function() {
            $(this).removeClass('selected');
        });
        $(this).addClass('selected');
    });

    //update currently table database
    $( document ).on( "click", "#table-list .list-group-item", function() {
        $("#table-list .list-group-item").each(function() {
            $(this).removeClass('selected');
        });
        $(this).addClass('selected');
    });


});

var isDatabaseSelected = true;

function getData(tableName) {

   $.ajax({url: "getAllDataFromTheTable?tableName="+tableName, success: function(result){

           result = JSON.parse(result);
           inflateData(result);

   }});

}

function queryFunction() {

   var query = $('#query').val();

   $.ajax({url: "query?query="+escape(query), success: function(result){

           result = JSON.parse(result);
           inflateData(result);

   }});

}

function downloadDb() {
    if (isDatabaseSelected) {
        $.ajax({url: "downloadDb", success: function(){
             window.location = 'downloadDb';
        }});
    }
}


function getDBList() {

   $.ajax({url: "getDbList", success: function(result){

           result = JSON.parse(result);
           var dbList = result.rows;
           $('#db-list').empty();
           var isSelectionDone = false;
           for(var count = 0; count < dbList.length; count++){
             if(dbList[count].indexOf("journal") == -1){
                $("#db-list").append("<a href='#' id=" +dbList[count] + " class='list-group-item' onClick='openDatabaseAndGetTableList(\""+ dbList[count] + "\");'>" +dbList[count] + "</a>");
                if(!isSelectionDone){
                    isSelectionDone = true;
                      $('#db-list').find('a').trigger('click');
                }
             }
           }

   }});

}

function openDatabaseAndGetTableList(db) {

    if("APP_SHARED_PREFERENCES" == db) {
        $('#run-query').removeClass('active');
        $('#run-query').addClass('disabled');
        $('#selected-db-info').removeClass('active');
        $('#selected-db-info').addClass('disabled');
        isDatabaseSelected = false;
        $("#selected-db-info").text("SharedPreferences");
    } else {
        $('#run-query').removeClass('disabled');
        $('#run-query').addClass('active');
        $('#selected-db-info').removeClass('disabled');
        $('#selected-db-info').addClass('active');
        isDatabaseSelected = true;
        $("#selected-db-info").text("Export Selected Database : "+db);
    }


   $.ajax({url: "getTableList?database="+db, success: function(result){

           result = JSON.parse(result);
           var tableList = result.rows;
           var dbVersion = result.dbVersion;
           if("APP_SHARED_PREFERENCES" != db) {
              $("#selected-db-info").text("Export Selected Database : "+db +" Version : "+dbVersion);
           }
           $('#table-list').empty()
           for(var count = 0; count < tableList.length; count++){
             var tableName = tableList[count];
             $("#table-list").append("<a href='#' data-db-name='"+db+"' data-table-name='"+tableName+"' class='list-group-item' onClick='getData(\""+ tableName + "\");'>" +tableName + "</a>");
           }

   }});

}

function inflateData(result){

   if(result.isSuccessful){
      showSuccessInfo();
      var columnHeader = result.tableInfos;

      // set function to return cell data for different usages like set, display, filter, search etc..
      for(var i = 0; i < columnHeader.length; i++) {
        columnHeader[i]['targets'] = i;
        columnHeader[i]['data'] = function(row, type, val, meta) {
            var dataType = row[meta.col].dataType;
            if (type == "sort" && dataType == "boolean") {
                return row[meta.col].value ? 1 : 0;
            }
            return row[meta.col].value;
        }
      }
      var columnData = result.rows;
       var tableId = "#db-data";
        if ($.fn.DataTable.isDataTable(tableId) ) {
          $(tableId).DataTable().destroy();
        }

       $("#db-data-div").remove();
       $("#parent-data-div").append('<div id="db-data-div"><table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered display" id="db-data"></table></div>');

       $(tableId).dataTable({
           "data": columnData,
           "columnDefs": columnHeader,
           'bPaginate': true,
           'searching': true,
           'bFilter': true,
           'bInfo': true,
           "bSort" : true,
           "scrollX": true,
           "iDisplayLength": 10,
           "dom": "Bfrtip",
            select: 'single',
            responsive: true,
            altEditor: true,     // Enable altEditor
            buttons: [
                {
                    extend: 'selected', // Bind to Selected row
                    text: 'Edit',
                    name: 'edit'        // do not change name
                }
            ]
       })

       //attach row-updated listener
       $(tableId).on('update-row.dt', function (e, updatedRowData, callback) {
            var updatedRowDataArray = JSON.parse(updatedRowData);
            //add value for each column
            var data = columnHeader;
            for(var i = 0; i < data.length; i++) {
                data[i].value = updatedRowDataArray[i].value;
                data[i].dataType = updatedRowDataArray[i].dataType;
            }
            //send update table data request to server
            updateTableData(data, callback);
       });
       // hack to fix alignment issue when scrollX is enabled
       $(".dataTables_scrollHeadInner").css({"width":"100%"});
       $(".table ").css({"width":"100%"});
   }else{
      showErrorInfo();
   }

}

//send update database request to server
function updateTableData(updatedData, callback) {
    //get currently selected element
    var selectedTableElement = $("#table-list .list-group-item.selected");

    var filteredUpdatedData = updatedData.map(function(columnData){
        return {
            title: columnData.title,
            isPrimary: columnData.isPrimary,
            value: columnData.value,
            dataType: columnData.dataType
        }
    });
    //build request parameters
    var requestParameters = {};
    requestParameters.dbName = selectedTableElement.attr('data-db-name');
    requestParameters.tableName = selectedTableElement.attr('data-table-name');;
    requestParameters.updatedData = encodeURIComponent(JSON.stringify(filteredUpdatedData));

    //execute request
    $.ajax({
        url: "updateTableData",
        type: 'GET',
        data: requestParameters,
        success: function(response) {
            console.log("Data updated successfully");
            callback(true);
        },
        error: function(xhr, status, error) {
            console.log("Data updated failed");
            callback(false);
        }
    })
}

function showSuccessInfo(){
    $("#success-info").show();
    $("#error-info").hide();
}

function showErrorInfo(){
    $("#success-info").hide();
    $("#error-info").show();
}

function hideBothInfo(){
    $("#success-info").hide();
    $("#error-info").hide();
}