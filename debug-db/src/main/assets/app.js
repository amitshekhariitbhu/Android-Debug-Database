$( document ).ready(function() {
    getDBList();
});

function getData(tableName) {

   $.ajax({url: "getAllDataFromTheTable?tableName="+tableName, success: function(result){

           result = JSON.parse(result);
           var columnHeader = result.columns.map(function(columnName) {
            return {"title": columnName};
           });

           var columnData = result.rows;
           var tableId = "#db-data";
            if ($.fn.DataTable.isDataTable(tableId) ) {
              var individualInstallDistributionTable = $(tableId).DataTable();
              individualInstallDistributionTable.destroy();
            }
           $(tableId).dataTable({
               "data": columnData,
               "columns": columnHeader,
               'bPaginate': true,
               'searching': true,
               'bFilter': false,
               'bInfo': false,
               "bSort" : false,
               "iDisplayLength": 10
           });

   }});

}

function queryFunction() {

   var query = $('#query').val();

   $.ajax({url: "query?query="+query, success: function(result){

           result = JSON.parse(result);
           var columnHeader = result.columns.map(function(columnName) {
            return {"title": columnName};
           });

           var columnData = result.rows;
           var tableId = "#db-data";
            if ($.fn.DataTable.isDataTable(tableId) ) {
              var individualInstallDistributionTable = $(tableId).DataTable();
              individualInstallDistributionTable.destroy();
            }
           $(tableId).dataTable({
               "data": columnData,
               "columns": columnHeader,
               'bPaginate': true,
               'searching': true,
               'bFilter': false,
               'bInfo': false,
               "bSort" : false,
               "iDisplayLength": 10
           });

   }});

}


function getDBList() {

   $.ajax({url: "getDbList", success: function(result){

           result = JSON.parse(result);
           var dbList = result.rows;
           $('#db-list').empty()
           for(var count = 0; count < dbList.length; count++){
             if(dbList[count].indexOf("journal") == -1){
                $("#db-list").append("<a href='#' class='list-group-item' onClick='openDatabaseAndGetTableList(\""+ dbList[count] + "\");'>" +dbList[count] + "</a>");
             }
           }

   }});

}

function openDatabaseAndGetTableList(db) {

   $.ajax({url: "getTableList?database="+db, success: function(result){

           result = JSON.parse(result);
           var tableList = result.rows;
           $('#table-list').empty()
           for(var count = 0; count < tableList.length; count++){
             $("#table-list").append("<a href='#' class='list-group-item' onClick='getData(\""+ tableList[count] + "\");'>" +tableList[count] + "</a>");
           }

   }});

}