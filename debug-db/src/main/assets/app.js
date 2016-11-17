$( document ).ready(function() {
    getDBList();
    $("#query").keypress(function(e){
        if(e.which == 13) {
            queryFunction();
        }
    });
});

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
    } else {
        $('#run-query').removeClass('disabled');
        $('#run-query').addClass('active');
    }

   $("#selected-db-info").text("Selected Database : "+db);

   $.ajax({url: "getTableList?database="+db, success: function(result){

           result = JSON.parse(result);
           var tableList = result.rows;
           $('#table-list').empty()
           for(var count = 0; count < tableList.length; count++){
             $("#table-list").append("<a href='#' class='list-group-item' onClick='getData(\""+ tableList[count] + "\");'>" +tableList[count] + "</a>");
           }

   }});

}

function inflateData(result){

   if(result.isSuccessful){
      showSuccessInfo();
      var columnHeader = result.columns.map(function(columnName) {
           return {"title": columnName};
       });
      var columnData = result.rows;
       var tableId = "#db-data";
        if ($.fn.DataTable.isDataTable(tableId) ) {
          $(tableId).DataTable().destroy();
        }

       $("#db-data-div").remove();
       $("#parent-data-div").append('<div id="db-data-div"><table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered display" id="db-data"></table></div>');

       $(tableId).dataTable({
           "data": columnData,
           "columns": columnHeader,
           'bPaginate': true,
           'searching': true,
           'bFilter': true,
           'bInfo': true,
           "bSort" : true,
           "scrollX": true,
           "iDisplayLength": 10
       });
       // hack to fix alignment issue when scrollX is enabled
       $(".dataTables_scrollHeadInner").css({"width":"100%"});
       $(".table ").css({"width":"100%"});
   }else{
      showErrorInfo();
   }

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