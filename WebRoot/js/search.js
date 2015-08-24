$( document ).delegate("#pageSearch","pageinit",
	  function(){

          $($("#selectSearchOption + div > span")[0])
               .text(TEXT_FLIP_OPTION_USER);

          $($("#selectSearchOption + div > span")[1])
               .text(TEXT_FLIP_OPTION_MESSAGE);

          $("#searchHelp").text(TEXT_SEARCH_HELP);
      });

$(document).delegate("#pageSearch","pagebeforeshow",function(){

    var searchType=$("#pageSearch").jqmData(SEARCH_TYPE_KEY);

    var toSearch;

    if(searchType==SEARCH_USER_NAME_KEY){
	 toSearch=$("#pageSearch").jqmData(SEARCH_USER_NAME_KEY);
    }else if(searchType==SEARCH_TOPIC_KEY){
	 toSearch=$("#pageSearch").jqmData(SEARCH_TOPIC_KEY);
    }

    $("#pageSearch").jqmRemoveData(SEARCH_TYPE_KEY);


    if(toSearch!= undefined && toSearch!=null){
	 clearSearchResult();
    }

    $("#inputSearch").val(toSearch);


    if(searchType==SEARCH_USER_NAME_KEY){

	 $("#selectSearchOption").val("user").slider("refresh");
	 $("#pageSearch").jqmRemoveData(SEARCH_USER_NAME_KEY);

	 sendSearchQuery("user",{
	      searchType:"EXACTLY",
	      keywords:toSearch
	 });

    }else if(searchType==SEARCH_TOPIC_KEY){

	 $("#selectSearchOption").val("message").slider("refresh");
	 $("#pageSearch").jqmRemoveData(SEARCH_TOPIC_KEY);


	 sendQueryTopic(toSearch.substring(1,toSearch.length-1));

    }

});

function clearSearchResult(){
    var searchResultList=$("#searchResultList");
    searchResultList.html("");
    resultIdList.clear();
}

var resultIdList={
     idList_:new Array(),

     append:function(message){
	  if(this.idList_.indexOf(message.idBytes)!=-1){
	       //already had
	       return false;
	  }else{
	       this.idList_.push(message.idBytes);
	       return true;
	  }
	    
     },

     clear:function(){
		this.idList_.length=0;
	   }

};


function appendQueryMessageResults(messageList){

    var searchResultList=$("#searchResultList");

    for (var ele in messageList) {			

        var thisMessage=messageList[ele];

	if(!resultIdList.append(thisMessage)){
	     //already in result list
	     continue;
	}

        var toAppend=createMessageBlogHtml(thisMessage,MESSAGE_ID_SUFFIX);

        toAppend.dom.prepend(searchResultList).enhanceWithin();

    }

    searchResultList.listview('refresh');
}

function appendQueryUserResults(userList){

    var searchResultList=$("#searchResultList");

    for (var ele in userList) {			

        var thisUser=userList[ele];

        var fillText=$.sprintf(TEXT_USER_QUERY_RESULT_ROW,
                thisUser.createDate,
                thisUser.talkNumber,
                thisUser.description);

        var htmlText=$.sprintf(FORMAT_USER_QUERY_RESULT_ROW,
                thisUser.userName,fillText);

        searchResultList.prepend(htmlText);
    }

    searchResultList.listview('refresh');
}

function sendSearchQuery(type,queryObject,afterFillCallback){

    var servletUrl=(type=="user"?
            "/QueryUserServlet":"/QueryMessageServlet");

    var ajaxOption={
        url : servletUrl,
        beforeSend : function() {
	     $.mobile.loading('show', {
		  theme: "a",
	     text: "loading...",
	     textonly: true,
	     textVisible: true
	     });


            $("#inputSearch").textinput("disable");
        },
        complete : function() {
	      $.mobile.loading( "hide" );
                       $("#inputSearch").textinput("enable");
                   },
        type : "POST",
        data : JSON.stringify(queryObject),
	dataType: "json",
        success : function(jsonResult){
            if(jsonResult.success){
                if(type=="user"){
                    appendQueryUserResults(jsonResult.detail);
                }else{
                    appendQueryMessageResults(jsonResult.detail);
                }

		if(afterFillCallback!=undefined && (typeof afterFillCallback === "function")){
		     afterFillCallback();
		}

            }else{
		        displayError(jsonResult.detail);
            }
        },
        error : function() {
                    toast("network error");
                }

    };

    $.ajax(ajaxOption);
}

// when user click Enter in search bar
function onInputSearchChanged(){
    var searchValue=$("#inputSearch").val();

    if(searchValue=="" || searchValue==null){
	 //do not allow empty search
        return;
    }

    clearSearchResult();

    //clear prev result

    // get search option (query User or query Talk)
    var searchOption=$("#selectSearchOption").val();

    sendSearchQuery(searchOption,{
        searchType: "FUZZY",
        keywords: searchValue
    });
}
