"use strict";


//TODO get from global info
var VOTE_OPTION_MAX_NUMBER=6;

var VOTE_OPTION_DEFAULT_NUMBER=3;
var VOTE_TOPIC_ID_BYTES_KEY="VOTE_TOPIC_ID_BYTES";
var VOTE_OF_ME_OPTIONS_KEY="VOTE_OF_ME_OPTIONS";
var VOTE_TOPIC_FIELD_SET_TEMP_ID_KEY="VOTE_TOPIC_FIELD_SET_TEMP_ID";
var MIN_LAZY_PANEL_HEIGHT=100;
var MESSAGE_PANEL_SAFE_GAP=20;
var REFERENCE_ID_BYTES_KEY="REFERENCE_ID_BYTES";
var REFERENCE_USER_NAME_KEY="REFERENCE_USER_NAME";
var REFERENCE_IS_SHORT_KEY="REFERENCE_IS_SHORT";
var REFERENCE_TEXT_KEY="REFERENCE_TEXT";
var IS_BROADCAST_KEY="IS_BROADCAST";
var IS_MY_TALK_KEY="IS_MY_TALK";
var INNER_MOST_REFERENCE_ID_BYTES_KEY="INNER_MOST_REFERENCE_ID_BYTES";

var MESSAGE_ID_SUFFIX="_messagePage";

var messagePageOption={
     //if chat mode auto scroll to last message
     chatAutoScroll_:true,

     chatAutoScroll:function(set){
	  if(set!=null && set!=undefined){
	       this.chatAutoScroll_=set;
	  }
	  return this.chatAutoScroll_;
     },


};

//only cache newest messages ,do not deal with search list
var messageLocalCache={
		// how many items keeps in cache
		maxKeep_:100,
		
		waitForInput_ : true,
		
		lastestMessageDate_ : new Date(1970,1,1,0,0,0),
		
		// newest item first
		cachedList_:[],
		
		lastestMessageDate:function(newDate){
			if(newDate!=undefined){
				var tryConvert=$.format.date(newDate);				
				
				if(tryConvert!=null){
					this.lastestMessageDate_=tryConvert;
				}
			}
			
			return this.lastestMessageDate_;
		},
		
		waitForInput :function(state){
			if(state!=null && state!=undefined){
				this.waitForInput_=state;
			}
			return this.waitForInput_;
		},

		maxKeep:function(){
			     return this.maxKeep_;
			},
		
		append:function(lazyMessageList){
			
			// shrink first
			var shrinkMax=Math.max(0,this.maxKeep_-lazyMessageList.length);
			// remove head
			var startIndex=this.cachedList_.length-Math.min(shrinkMax,this.cachedList_.length);
			this.cachedList_=this.cachedList_.slice(startIndex,this.cachedList_.length);

			//TODO remove same id and date before function
			
			var cancatSize=this.maxKeep_-this.cachedList_.length;
			this.cachedList_=this.cachedList_.concat(lazyMessageList.slice(lazyMessageList.length-cancatSize,lazyMessageList.length));
			
			// assume newest list not exclude max keep
			this.cachedList_.length=Math.min(this.maxKeep_,this.cachedList_.length);
			
		},
		
		 lastestList:function(number){
		   	var beginIndex=Math.max(0,this.cachedList_.length-number);
			return this.cachedList_.slice(beginIndex,this.cachedList_.length);
		}
		
}; 

var pollingState={

     currentMode:function(newMode)
     {

	  if(newMode!=null&& newMode!=undefined){
	       this.currentMode_=newMode;
	  }
	  return this.currentMode_;
     },

     autoLoad:function(set){
		   if(set!=null && set!=undefined){
			this.autoLoad_=set;
		   }
		   if(this.autoLoad_){
			$.mobile.loading('show', {
			     theme: "a",
			     text: "loading...",
			     textonly: true,
			     textVisible: true
			});

            //must trigger create ,or switch from refreshed session page 
            //leads 'call disable before initialization' error
                   $("#buttonRefresh").addClass("ui-disabled");
		   }else{
			 $.mobile.loading( "hide" );
            if(this.currentMode_=="LAZY"){
		 $("#buttonRefresh").removeClass("ui-disabled");
            }
		   }
		   return this.autoLoad_;
	      },

     completeOk:function(set){
	  if(set!=null && set!=undefined ){
	       if(set){
		    this.adaptiveDelay_=0;
	       }else{
		    this.adaptiveDelay_=Math.min(
                    Math.max(this.adaptiveDelay_,1000)*2,20000);
	       }
	  }
	  return this.adaptiveDelay_;
     },

     autoLoad_:true,
           
     currentMode_:"LAZY",

     adaptiveDelay_:0

};

var FILE_SHARE_KEY="FILE_SHARE";

function makeInputTalkReadyState(){
	messageLocalCache.waitForInput(true);
	$("#inputTalk").val("");
	$("#imgSharePreview").hide();
    $("#inputTalk").prop("placeholder",INPUT_TALK_WAIT_TEXT);

	$("#fileupload").jqmRemoveData(FILE_SHARE_KEY);
	$("#progressbar").delay(1200).fadeOut(400, function() {
		$(this).hide();
		jQMProgressBar('progressbar').setValue(0);
	});


	//hide preview reference message
	$("#textReferenceMessagePreview").val("").hide();
	//clear reference id data

	$("#inputTalk").jqmRemoveData(REFERENCE_ID_BYTES_KEY);
	$("#inputTalk").jqmRemoveData(INNER_MOST_REFERENCE_ID_BYTES_KEY);
	$("#inputTalk").jqmRemoveData(IS_BROADCAST_KEY);
	$("#inputTalk").jqmRemoveData(VOTE_OF_ME_OPTIONS_KEY);
	$("#inputTalk").jqmRemoveData(VOTE_TOPIC_FIELD_SET_TEMP_ID_KEY);

    //if we are in vote state, clear it
    //
    if($("#divVoteContainer").is(":visible")){
        clearVoteTopicInput();
        $("#divVoteContainer").hide();
        resizeLazyPanel();
    }
}

function onButtonRefreshClicked(){

    var unread=$("#buttonRefresh").jqmData(UNREAD_KEY);
     if(unread>0){

	  clearAndFillLazyList();

	  $("buttonRefresh").jqmData(UNREAD_KEY,0);
	  return;
     }

    pollingState.autoLoad(true);
    polling(true,"REFRESH");
}


$( document ).delegate("#pageMessage","pageshow",
	  function(){

          resizeLazyPanel();
	       if(pollingState.autoLoad()){

		    $.mobile.loading('show', {
			 theme: "a",
			 text: "loading...",
			 textonly: true,
			 textVisible: true
		    });

	       }
	  }
	  );
function resizeLazyPanel(){
	
    var talkRect=$("#divCollapseTalk")[0].getBoundingClientRect();

    var footerRect=$("#footer")[0].getBoundingClientRect();

    // calc padding and margin
    var height=footerRect.top-talkRect.bottom- MESSAGE_PANEL_SAFE_GAP;

    //at least 
    height=Math.max(height,MIN_LAZY_PANEL_HEIGHT);
        
    $("#lazyPanel").height(height).nanoScroller();
	
}

function resizeEagerPanel(){

    var talkRect=$("#divCollapseTalk")[0].getBoundingClientRect();
    var footerRect=$("#footer")[0].getBoundingClientRect();
    var headerRect=$("#header")[0].getBoundingClientRect();

    var height=footerRect.top-headerRect.bottom-talkRect.height-MESSAGE_PANEL_SAFE_GAP;

    $("#eagerPanel").height(height).nanoScroller();
}

$( document ).delegate("#pageMessage","pagecreate",
		function(){
   
	//first disable this button, after lazy loading ,button will be enabled again
	$("#buttonRefresh").text(BUTTON_REFRESH);

    $("#divVoteContainer").hide();
	
	$("#buttonRefresh").jqmData(UNREAD_KEY,0);
	
	 //translate string
	 $("#buttonTalk").val(BUTTON_TALK).button("refresh");

	 $("#buttonView").val(BUTTON_VIEW).button("refresh");

	 $("#buttonLater").val(BUTTON_LATER).button("refresh");

	 $("#buttonPollSwitch").text(BUTTON_POLL_SWITCH_CHAT);

	 $("#buttonVote").val(BUTTON_VOTE).button("refresh");

	 $("#buttonUpload").val(BUTTON_UPLOAD).button("refresh");

	 $("#buttonMention").val(BUTTON_MENTION).button("refresh");

	 $("#buttonTopic").val(BUTTON_TOPIC).button("refresh");

	 $("#buttonLongTalk").val(BUTTON_LONG_TALK).button("refresh");

	 $("#inputTalk").prop("placeholder",INPUT_TALK_WAIT_TEXT);

     $("#titleConfirmClear").text(TITLE_CONFIRM_CLEAR);

     $("#contentConfirmClear").text(CONTENT_CONFIRM_CLEAR);

     $("#inputVoteTopicTopic").prop("placeholder",INPUT_VOTE_TOPIC_WAIT_TEXT);

     updateVoteOptionPlaceHolder();

     //http://stackoverflow.com/questions/6348494/addeventlistener-vs-onclick
     $("#listViewVoteOptions input + a ").on("click",onVoteOptionClear);

	 $("#buttonClear").val(BUTTON_CLEAR).button("refresh");

     $("#buttonConfirmClear").val(BUTTON_CLEAR).button("refresh");
     $("#buttonCancelClear").val(BUTTON_CANCEL).button("refresh");
     $("#buttonClearAndNotRemind").val(BUTTON_CLEAR_AND_NOT_REMIND).button("refresh");

     changeCollapseTalkTitle(TEXT_EXPAND); 

     //http://stackoverflow.com/questions/21067210/add-custom-animation-to-collapsible-set-in-jquery-mobile
     $("#divCollapseTalk").collapsible({
         collapse: function(event,ui){
             $(this).children().next().slideUp(150,resizeMessagePanel);
             changeCollapseTalkTitle(TEXT_EXPAND); 
         },
         expand: function(event,ui){
             $(this).children().next().hide();
             $(this).children().next().slideDown(150,resizeMessagePanel);
             changeCollapseTalkTitle(TEXT_COLLAPSE); 
         }});

     //header default use a gray background,makes
     //input likes disabled. this correct it
     $("#divVoteHeader div.ui-controlgroup-controls div.ui-input-text").css("background","#FFF");


     //seems jqm generate reverse order span
     $($("#flipVoteMultiSelection + div > span")[0]).
         text(TEXT_FLIP_OPTION_VOTE_MULTI);
     $($("#flipVoteMultiSelection + div > span")[1]).
         text(TEXT_FLIP_OPTION_VOTE_SINGLE);



	 $('#titleTalk .ui-btn-text').text(TITLE_TALK);

	 if(messageLocalCache.waitForInput()){
		 makeInputTalkReadyState();
	 }

	 $("#popupUnread").popup("option",
             {transition:"pop",
                 positionTo:$("#buttonRefresh")
             })
         .on("popupafteropen",function(){
	      setTimeout(function(){$("#popupUnread").popup("close");},4000);
	 });





	 //pause media play before play window close
	 $("#popupAudioPlayer").bind({
	      popupafterclose:function(ev,ui){
				   $("#audio_jplayer").jPlayer("pause");
				   $("#audio_jplayer").jPlayer("destroy");
			      }
	 });
	
	 $("#popupVideoPlayer").bind({
	      popupafterclose:function(ev,ui){
				   $("#video_jplayer").jPlayer("pause");
				   $("#video_jplayer").jPlayer("destroy");
			      }
	 }); 
     //message menu popup i18n

         $("#linkBroadcastOrigin").text(LINK_BROADCAST_ORIGIN);
         $("#linkBroadcast").text(LINK_BROADCAST);
         $("#linkComment").text(LINK_COMMENT);
         $("#linkViewAuthor").text(LINK_VIEW_AUTHOR);
         $("#linkDelete").text(LINK_DELETE);

	 //only show when user upload file
	 $("#imgSharePreview").hide();

     $('#fileupload').fileupload({
	  sequentialUploads: true,
	  autoUpload:false,
	  progress:function(e,data){
	        var progress = parseInt(data.loaded / data.total * 100, 10);
		jQMProgressBar('progressbar').setValue(progress);
	  }
     });

     $("#progressbar").hide();

     jQMProgressBar('progressbar')
	  .setOuterTheme('b')
	  .setInnerTheme('e')
	  .isMini(true)
	  .setMax(100)
	  .setStartFrom(0)
	  .showCounter(true)
	  .build();

	 //init scroll view
	 var viewportHeight = document.documentElement.clientHeight;
	 overthrow.set();


	 $("#eagerPanel").hide();

	 pollingState.autoLoad(true);
	 polling(false,"HISTORY");
	
});

function onButtonUploadClicked(){
    $("#fileupload").click();
}


function changeCollapseTalkTitle(newTitle){

    var title=$("#divCollapseTalk a")[0]; 
    var inner=$(title.children); 
    $(title).text(newTitle).append(inner); 
}

function resizeMessagePanel(){

    if(pollingState.currentMode()=="EAGER"){
        resizeEagerPanel();
    }else{
        resizeLazyPanel();
    }
}

function polling(onlyOnce,pollingTypeOuter){


     (function impl(pollingTypeInner){

	  var pollTypeThis=(pollingTypeInner==null?pollingState.currentMode():pollingTypeInner);


         if(GlobalInfo.isLogined()){

     $.ajax({
		url : "/PollServlet",
		complete : function() {
		     if(!onlyOnce){
			  setTimeout(impl,pollingState.completeOk());
		     }
		},

	  	timeout: 35000,

		type : "POST",
		success : function(jsonResult){

		    
		     pollingState.completeOk(jsonResult.success);
             if (jsonResult.success) {
                 if(jsonResult.detail.length>0){

                     loadMessagesResults(jsonResult.detail);
                 }else if(pollingTypeInner!="REFRESH"){
                     pollingState.autoLoad(false);
                 }
             }else{
                 displayError(jsonResult.detail);
             }
	      $.mobile.loading( "hide" );
		},
		dataType : "json",

		data : JSON.stringify({
		     pollType : pollTypeThis,
		}),
		
		error : function(xmlHttpRequest, textStatus, errorThrown){
                    if((xmlHttpRequest.readyState == 0 || 
                            xmlHttpRequest.status == 0)
                        &&(textStatus==""))
                        // it's not really an error
                    {return;}
            //too many wait time
		    if(pollingState.completeOk(false)>10000){
                //TODO check if current page is message?
                toast("network error");
            }
		},
		
	});}else if(!onlyOnce){
        //not logined,delay poll
        setTimeout(function(){impl(pollTypeThis);},pollingState.completeOk(false));
    }
     })(pollingTypeOuter);
}


function onInputTalk(){
	if(messageLocalCache.waitForInput()){
		$("#inputTalk").val("");		
	}
}

function onLinkPopupViewImageShareClicked(imgSrc){

     $("#imgViewImageShare").prop('src',"upload/"+imgSrc);

     $("#popupViewImageShare").popup("open");

}

var firstHistoryList=true;

function appendBlog(allNewMessageList){

     var containerElement=$("#lazyMessageList");

     var beginIndex=Math.max(0,allNewMessageList.length-messageLocalCache.maxKeep());

     var newMessageList=allNewMessageList.slice(beginIndex);
     
     var thisIsFirstHistory=firstHistoryList;
     firstHistoryList=false;

     for (var ele in newMessageList) {			

	  var thisMessage=newMessageList[ele];

      if(thisMessage.idBytes=="0000000000000000000000000000000000000002"){
          continue;
      }

      var toAppend=createMessageBlogHtml(thisMessage,MESSAGE_ID_SUFFIX,thisIsFirstHistory);

      toAppend.dom.prependTo(containerElement).enhanceWithin();

      var prependRow=$("#"+createSuffixedId(thisMessage.idBytes,MESSAGE_ID_SUFFIX));
      //TODO check message type. skip if this is a vote message
      prependRow.jqmData(IS_BROADCAST_KEY,toAppend.isBroadcast);
      //TODO should has a special class in html node,ant extract 
      //plain text from this node
      prependRow.jqmData(REFERENCE_TEXT_KEY,
              thisMessage.plainText);

      prependRow.jqmData(IS_MY_TALK_KEY,
              toAppend.isMyTalk);

      prependRow.jqmData(INNER_MOST_REFERENCE_ID_BYTES_KEY,
              toAppend.innerMostReferenceIdBytes);

	  if(ele==newMessageList.length-1){
	       messageLocalCache.lastestMessageDate(thisMessage.createDate);
	  }
     }

     containerElement.listview("refresh");

     //init image function
     $(".fancybox").fancybox({
         closeBtn		: true,
         padding: 1,
         margin: 8
     });

     //scroll to top check list size
     $("#lazyPanel").nanoScroller();
}

function clearAndFillLazyList(){

     var displayLimit=40;

     var newest=messageLocalCache.lastestList(displayLimit);

     var containerElement=$("#lazyMessageList");

     containerElement.empty();

     appendBlog(newest);

}

function appendChat(newMessageList){

    //TODO different message type

     var containerElement=$("#eagerMessageList");

     for (var ele in newMessageList ) {			

	  var msg=newMessageList[ele];

      if(msg.idBytes=="0000000000000000000000000000000000000002"){
          continue;
      }

	  jQuery('<dt/>',{
	       "class":"eager-message-dt",
	       text:msg.sender.userName
	  }).appendTo(containerElement);

	  jQuery('<dd/>',{
	       "class":"eager-message-dd",
	       text:msg.plainText
	  }).appendTo(containerElement);


	  //TODO
	  if(ele==newMessageList.length-1){
	       messageLocalCache.lastestMessageDate(newMessageList[ele].createDate);
	  }
     }

     $("#eagerPanel").nanoScroller();

     if(messagePageOption.chatAutoScroll()){
	  scrollToEagerBottom();
     }
}

var UNREAD_KEY="unread";

function loadMessagesResults(newMessageList) {

     messageLocalCache.append(newMessageList);

     if(pollingState.currentMode()=="EAGER"){
	  appendChat(newMessageList);
	  return;
     }

	       
     //this is user triggered refresh
     if (pollingState.autoLoad()){
	  appendBlog(newMessageList);
      scrollToLazyTop();
	  pollingState.autoLoad(false);
	  return;
     }



     var unread=$("#buttonRefresh").jqmData(UNREAD_KEY)+newMessageList.length;

     if(unread>0){

         $("#textPopupUnread").text(
                 $.sprintf(TEXT_POPUP_UNREAD,unread));

         $("#popupUnread").popup("open");
         $("#buttonRefresh").jqmData(UNREAD_KEY,unread);
     }

}

function onHasInput(){
	messageLocalCache.waitForInput(false);
}

function scrollToLazyTop(){

     var messageList=$("#lazyMessageList");
     if(messageList.length>0){
	  $("#lazyPanel .content").stop().animate({'scrollTop':messageList.get(0).offsetTop},900,'swing');
     }
}

function scrollToEagerBottom(){

     var messageList=$("#eagerMessageList");
     if(messageList.length>0){
     $("#eagerPanel .content").stop().
	  animate({'scrollTop':$("#eagerMessageList :last-child").get(0).offsetTop},900,'swing');
     }
}

function onButtonViewClicked(){
    //TODO needs optimize .store unread messages to UNREAD_KEY
	  clearAndFillLazyList();
	  $("#buttonRefresh").jqmData(UNREAD_KEY,0);
      $("#popupUnread").popup("close");
      scrollToLazyTop();
}

function checkValidVoteTopic(){


    var totalLength=0;

    if(isEmptyString($("#inputVoteTopicTopic"))){

        return ERROR_VOTE_TOPIC_INVALID;
    }

    totalLength += $("#inputVoteTopicTopic").val().length;

    var ulli=$("#listViewVoteOptions input");

    var validOption = 0;

    for(var i=0;i<ulli.length;++i){

        var thisOption=$(ulli[i]);
        if(!isEmptyString(thisOption.val())){
            validOption+=1;
            totalLength+=thisOption.length;
        }
    }

    if(validOption<=1){
        return ERROR_VOTE_OPTION_INVALID;
    }

    var message_length_limit=65500;

    if(totalLength >= 65500){
        //TODO including json char

        return sprintf(ERROR_MESSAGE_TOO_LONG,message_length_limit);
    }

    return null;

}

function onButtonTalkClicked(){

    var talkJson;

    if($("#divVoteContainer").is(":visible")){
        //vot topic 

        var error=checkValidVoteTopic($("#inputVoteTopicTopic"));
        if(error!=null){
            displayError(error);
            return;
        }

        var results=new Object();

        var ulli=$("#listViewVoteOptions input");

        for(var i=0,j=0;i<ulli.length;++i){
            var thisOption=$(ulli[i]).val();
            if(!isEmptyString(thisOption)){
                results[thisOption]=0;
            }
        }

        talkJson={
            messageType: "VOTE_TOPIC",
            voteTopic: $("#inputVoteTopicTopic").val(),
            results: results,
            description: $("#inputTalk").val(),
            multiSelection : $("#flipVoteMultiSelection").val()=="multi"
        };


    }else if($("#inputTalk").jqmData(VOTE_OF_ME_OPTIONS_KEY)) {
        //is VoteOfMe message
        //
        //disable all same votetopic message 
        
        var voteTopicIdBytes=$("#inputTalk").jqmData(REFERENCE_ID_BYTES_KEY);

        //jqmData selector not work. jquery only support :data(attr) 
        //so we manual iterate every one
        //https://stackoverflow.com/questions/2891452/jquery-data-selector

        $("fieldset:data("+VOTE_TOPIC_ID_BYTES_KEY+")").each(function(idx,ele){

            var fieldset=$(ele);

            if(fieldset.data(VOTE_TOPIC_ID_BYTES_KEY)==voteTopicIdBytes){

                fieldset.find("input[type='radio']").checkboxradio("disable");
            }

        });

        var options=$("#inputTalk").jqmData(VOTE_OF_ME_OPTIONS_KEY);

        talkJson={
            messageType: "VOTE_OF_ME",
            "options":options,
            comment:$("#inputTalk").val()
        };

    }else if(messageLocalCache.waitForInput()){
		$("#inputTalk").val("");
		toast(TALK_SHOULD_NOT_EMPTY);
		return;
    }else{
        //plainText message
        var talkContent=$("#inputTalk").val();
        if(talkContent==null || isEmptyString(talkContent)){
            toast(TALK_SHOULD_NOT_EMPTY);
            return;
        }
        talkJson={
            plainText : talkContent,
            messageType : "PLAIN_TEXT"
        };
    }
	
	
	
	
	
    var ajaxOption={
        url : "/TalkServlet",
        beforeSend : function() {
	     $.mobile.loading('show', {
		  theme: "a",
	     text: "loading...",
	     textonly: true,
	     textVisible: true
	     });


            $("#buttonTalk").button("disable");
        },
        complete : function() {

	      $.mobile.loading( "hide" );
                       $("#buttonTalk").button("enable");
                   },
        type : "POST",
	dataType: "json",
        success : function(jsonResult){
            if(jsonResult.success){
                if(pollingState.currentMode()=="LAZY"){
                    toast(TALK_SUCCESS_TEXT);
                    pollingState.autoLoad(true);
                    polling(true,"REFRESH");
                }
                makeInputTalkReadyState();
            }else{
		displayError(jsonResult.detail);
            }
        },
        error : function() {
                    toast("network error");
                }

    };

    	 
    var referenceIdBytes=$("#inputTalk").jqmData(REFERENCE_ID_BYTES_KEY);
    if(referenceIdBytes!=null){
	 
	 if(!verifyIdBytes(referenceIdBytes)){
	      toast(ERROR_REFERENCE_ID_BYTES);
          return;
	 }

	 talkJson["referenceIdBytes"]=referenceIdBytes;
    }

    var textJsonString=JSON.stringify(talkJson);

    var shareFile=$("#fileupload").jqmData(FILE_SHARE_KEY);

    if(shareFile !=null && shareFile !=undefined ){
        //upload file,
        //post as form key-value
	

	 $("#progressbar").show();

        ajaxOption.files=[shareFile];
        ajaxOption.formData={PLAIN_TEXT:textJsonString};
	ajaxOption.autoUpload=true;

        $("#fileupload").fileupload('add',ajaxOption);

    }else{
        ajaxOption['data']=textJsonString;

        //post as entity
        $.ajax(ajaxOption);
    }

	
}

function onButtonLaterClicked(){
    $("#popupUnread").popup("close");
}

function clearAndFillEagerList(){

     $("#eagerMessageList").html("");

     var newest=messageLocalCache.lastestList(100);

     var autoScrollOption=messagePageOption.chatAutoScroll();

     messagePageOption.chatAutoScroll(false);

     appendChat(newest);

     messagePageOption.chatAutoScroll(autoScrollOption);

     $("#eagerPanel").nanoScroller({scroll:'bottom'});

}
	
/**
 *  switch between eager(im like) and lazy (twitter like) mode
*
*/
function onButtonPollSwitchClicked(){

     var toMode="EAGER";
     var hideEager=false;

     if(pollingState.currentMode()=="EAGER"){
	  toMode="LAZY";
	  hideEager=true;
     }

     pollingState.currentMode(toMode);

      $("#buttonRefresh").jqmData(UNREAD_KEY,0);

      $.mobile.loading('show', {
	   theme: "a",
	   text: "loading...",
	   textonly: true,
	   textVisible: true
      });





     if(hideEager){
	  $("#eagerPanel").hide();
	  $("#talkFunction").show();
	  $("#buttonPollSwitch").text(BUTTON_POLL_SWITCH_CHAT);
	  $("#buttonRefresh").removeClass("ui-disabled");
	  $("#titleTalk").show();
	  $("#divCollapseTalk").collapsible("collapse");
	  $("#lazyPanel").show();
	  clearAndFillLazyList();
	  scrollToLazyTop();
     }else{
	  $("#eagerMessageList").empty();
	  $("#eagerPanel").show();
	  $("#talkFunction").hide();
	  $("#buttonRefresh").addClass("ui-disabled");
	  $("#buttonPollSwitch").text(BUTTON_POLL_SWITCH_BLOG);
	  $("#divCollapseTalk").collapsible("expand");
	  $("#lazyPanel").hide();
	  $("#titleTalk").hide();
	  //must manual trigger this action to force LAZY polling return
	  polling(true,"EAGER");
	  clearAndFillEagerList();
	  scrollToEagerBottom();
     }

     $.mobile.loading( "hide" );

}

function loadShareImagePreview(file){
 
     var reader = new FileReader();

     $.mobile.loading('show', {
			     theme: "a",
			     text: "loading...",
			});

     reader.onload = function (e) {
	  $('#imgSharePreview').attr('src', e.target.result).show();
	  $("#imgSharePreview").show();
	  $.mobile.loading( "hide" );
     }

     reader.readAsDataURL(file);

}
	
function humanSizeFormat(byteNumber){
     if(byteNumber < 1024){
	  return byteNumber+' B';
     }else if(byteNumber < 1024*1024){
	  return $.sprintf("%.1f K",byteNumber/1024); 
     }else{
	  return $.sprintf("%.1f M",byteNumber/1024/1024); 
     }
}

function onFileShareChanged(fileEvent){

     var selectedFiles=fileEvent.target.files.length;

     if(selectedFiles==0){
	  return;
     }

     var file=fileEvent.target.files[0];

     var fileSize=file.size;

     if(fileSize>GlobalInfo.fileUploadLimitByte){
	  toast($.sprintf(ERROR_FILE_TOO_LARGE,
			 humanSizeFormat(fileSize),
			 humanSizeFormat(GlobalInfo.fileUploadLimitByte)));
	  return;
     }

     var predefinedTalk=TALK_SHARE_MESSAGE+file.name;


     if(/^image/.test(file.type)){
	  loadShareImagePreview(file);
	  predefinedTalk=TALK_SHARE_IMAGE_MESSAGE+file.name;
     }


     if(messageLocalCache.waitForInput()){
	  $("#inputTalk").val(predefinedTalk);
	  messageLocalCache.waitForInput(false);
     }

     $("#fileupload").jqmData(FILE_SHARE_KEY,file);

}

function onLinkPopupMessageMenuClicked(ele){

    //id is "messagePage" suffixed
    var messageRow=$(ele).closest("li.lazyMessageList-li");

    var messageRowId=messageRow.attr("id");

    var idBytes=extractIdBytesFromId(messageRowId);

    var jqueryEle=$(ele);

     $("#linkBroadcast").jqmData(REFERENCE_ID_BYTES_KEY,idBytes);

     var senderName=messageRow.find(".user-name-text").text();
          //user name is in this class
     $("#linkViewAuthor").jqmData(REFERENCE_USER_NAME_KEY,senderName);

    var element=$("#popupMessageMenu");
     element.popup();
     element.popup("option",{
         positionTo:"#"+messageRowId+" a:first",
         transition:"pop"
     });

     var isMyTalk=jqueryEle.jqmData(IS_MY_TALK_KEY);

     if(isMyTalk){
         $("li:has(#linkDelete)").show();
     }else{
         $("li:has(#linkDelete)").hide();
     }

     element.popup("open");
}

function onLinkCommentClicked(){
    //TODO
}

function onLinkBroadcastOriginClicked(){
    //TODO
}

function presetInputTalk(presetMessage){

    //http://stackoverflow.com/questions/22246686/jquery-mobile-to-check-if-a-div-is-expanded-or-collapsed
    if($("#divCollapseTalk").hasClass("ui-collapsible-collapsed")){
        $("#divCollapseTalk").collapsible("expand");
    }

    $("#inputTalk").val(presetMessage).trigger("keyup");

    messageLocalCache.waitForInput(false);
     $('html,body').stop().animate({scrollTop:0},500);
}

function extractPlainTextMessage(messageRowJquery){

    var isBroadcast=messageRowJquery.jqmData(IS_BROADCAST_KEY);

    var referenceUserName=messageRowJquery.find(".user-name-text:first").text();

    var referenceMessageText=messageRowJquery.find("p.plain-text:first").text();

    return {
	 
	 "isBroadcast":isBroadcast,
	 "sender":{"userName":referenceUserName},
	 "plainText":referenceMessageText
    };
 
}

function onLinkBroadcastClicked(ele){
    var jqueryEle=$(ele);

    $("#popupMessageMenu").popup("close");

    var idBytes=jqueryEle.jqmData(REFERENCE_ID_BYTES_KEY);

    var messageRow=$("#"+createSuffixedId(idBytes,MESSAGE_ID_SUFFIX));

    var message=extractPlainTextMessage(messageRow);

    //TODO if this is already a BROADCAST,should display a innerMost message

    var FOLLOWING_BROADCAST_FMT_STRING="||@%s %s";

    var fullMessage=message.isBroadcast?
	 $.sprintf(FOLLOWING_BROADCAST_FMT_STRING,message.sender.userName,message.plainText):
	 TEXT_BROADCAST;

    $("#textReferenceMessagePreview").html(
            $.sprintf(FORMAT_REFERENCE_MESSAGE_PREVIEW,
	      TEXT_REFERENCE_MESSAGE_PREVIEW,
	      message.sender.userName,message.plainText)).show();

    //attach referenceIdBytes to input 
    $("#inputTalk").jqmData(REFERENCE_ID_BYTES_KEY,messageRow.jqmData(INNER_MOST_REFERENCE_ID_BYTES_KEY));

    $("#inputTalk").jqmData(IS_BROADCAST_KEY,message.isBroadcast);

    var presetMessage=fullMessage.slice(0,BROADCAST_LIMIT);

    presetInputTalk(presetMessage);

}

function onLinkViewAuthorClicked(ele){
     var jqueryElement=$(ele);
     var userName=jqueryElement.jqmData(REFERENCE_USER_NAME_KEY);

     $("#pageSearch").jqmData(SEARCH_TYPE_KEY,SEARCH_USER_NAME_KEY);
     $("#pageSearch").jqmData(SEARCH_USER_NAME_KEY,userName);
     $.mobile.changePage("#pageSearch");

}

function removeRow(idBytes){

     var messageRow=$("#"+createSuffixedId(idBytes,MESSAGE_ID_SUFFIX)).parents("li");

     messageRow.remove();
     $("#lazyMessageList").listview("refresh");
}

function onLinkDeleteClicked(ele){

    $("#popupMessageMenu").popup("close");

    var idBytes=$("#linkBroadcast").jqmData(REFERENCE_ID_BYTES_KEY);

    $.ajax({
        url : "/DeleteMessageServlet",
        beforeSend : function() {

	    $.mobile.loading('show', {
			     theme: "a",
			     text: "loading...",
			});


        },
        complete : function() {
			 $.mobile.loading( "hide" );
                   },
        type : "POST",
	dataType: "json",
	data:JSON.stringify({"idBytes":idBytes}),
        success : function(jsonResult){
			 
	     $.mobile.loading( "hide" );
            if(jsonResult.success){
		 toast(TEXT_DELETE_SUCCESS);
		 //remove message from listview
		 removeRow(idBytes);
            }else{
		displayError(jsonResult.detail);
            }
        },
        error : function() {
                    toast("network error");
                }
    });
}

function onButtonVoteClicked(){
    $("#divVoteContainer").show();
    $("#inputTalk").prop("placeholder",INPUT_VOTE_TOPIC_DESCRIPTION_WAIT_TEXT);
    resizeLazyPanel();
}

var remindBeforeClear=true;

function onButtonClearAndNotRemindClicked(){
    remindBeforeClear=false;
    //deliver to common
    onButtonConfirmClearClicked();
}

function onButtonConfirmClearClicked(){

    var voteTopicFieldSetTempId=$("#inputTalk").jqmData(VOTE_TOPIC_FIELD_SET_TEMP_ID_KEY);

    //clear VoteTopic selection if any
    if(voteTopicFieldSetTempId!=null){
        //user did a VoteOfMe, but decide to give up
        //clear the selection
        //TODO
        //here comes a problem, if user already voted on same topic
        //previously, but scroll down to find same VoteTopic (which displayed
        //as un-voted), vote on it then clear, this action will clear
        //the already voted result selection too.
        //
        //we should change the 
        //already voted message to avoid this problem (including all in list)
        //(maybe not important, since user may not scroll down to see
        //previous message, or any refresh action gives all voted message)
    
        var whichFieldSetVoteTopic=$("#"+voteTopicFieldSetTempId);

        whichFieldSetVoteTopic.find("input").prop("checked",false).checkboxradio("refresh");

        //clear id
        whichFieldSetVoteTopic.attr("id",null);
        //TODO makeInputTalkReadState didn't clear the temp id 
        //didn't care it
    }

    makeInputTalkReadyState();
    $("#popupConfirmClear").popup("close");

}

function onButtonCancelClearClicked(){
    $("#popupConfirmClear").popup("close");
}

function inputVoteHasContent(){

    if($("#inputVoteTopicTopic").val()!=""){
        return true;
    }

    var ul=$("#listViewVoteOptions input");

    for(var i=0;i<ul.length;++i){
        if($(ul[i]).val()!=""){
            return true
        }
    }

    return $("#inputTalk").val()!="";

 
}

function onButtonClearClicked(){

    var isVoteTopicState=$("#divVoteContainer").is(":visible");

    var isVoteOfMeState=($("#inputTalk").jqmData(VOTE_OF_ME_OPTIONS_KEY)!=null);

    if($("#inputTalk").val()=="" && !isVoteTopicState && !isVoteOfMeState){
        return;
    }
    
    //vote state can be clear
    //
    if(isVoteTopicState && !inputVoteHasContent()){
        makeInputTalkReadyState();
        return;
        //has input, continue user confirm workflow
    }

    if(remindBeforeClear){
        $("#popupConfirmClear").popup("open");
    }else{
        makeInputTalkReadyState();
    }
}

function updateVoteOptionPlaceHolder(){

    var ulli=$("#listViewVoteOptions input");

    for(var i=0;i<ulli.length;++i){
        if ((i<VOTE_OPTION_DEFAULT_NUMBER) || (i!=ulli.length-1)){
            $(ulli[i]).prop("placeholder",
                    $.sprintf(INPUT_VOTE_TOPIC_OPTION_WAIT_TEXT,i+1));
        }else{
            $(ulli[i]).prop("placeholder",$.sprintf(
                        INPUT_VOTE_TOPIC_OPTION_WAIT_TEXT+INPUT_VOTE_TOPIC_OPTION_LIMIT_TEXT,
                    i+1,VOTE_OPTION_MAX_NUMBER));
        }
    }

}

function onVoteOptionKeyUp(keyboardEv){

    var jobj=$(keyboardEv.srcElement);

    var ul=$("#listViewVoteOptions");

    var ulli=ul.find("> li");
        

    if(jobj.val()!=""){
        //check if we need more



        //seems ul.find("input[value='']"); only apply to static 
        //elements, so must iterat manually

        if(ulli.length==VOTE_OPTION_MAX_NUMBER){
            return;
        }

        for(var i=0;i<ulli.length;++i){

            var checkLi=$(ulli[i]);

            if(checkLi.find("input")[0]==keyboardEv.srcElement){
                //exclude self out of check
                //or it may have val()=="" (key event not display on element yet)
                continue;
            }
            if(checkLi.find("input").val()==""){
                return;
            }
        }
        

        //need more
        

        ul.append(FORMAT_VOTE_OPTION_ROW);

        ul.find(">li:last-child").trigger('create');

        ul.find(">li:last-child a").on("click",onVoteOptionClear);

        updateVoteOptionPlaceHolder();
        ul.listview("refresh");
        resizeLazyPanel();

        return;
    }

    //remove extra 


    if(ulli.length<=VOTE_OPTION_DEFAULT_NUMBER){
        return;
    }

    //remove from tail
    for(var i=ulli.length-1;i>=0;--i){

        var checkLi=$(ulli[i]);

        if(checkLi.find("input").val()==""){
            checkLi.remove();
            ul.listview("refresh");
            break;
        }
    }

    updateVoteOptionPlaceHolder();
    resizeLazyPanel();
}

function onVoteOptionClear(ev){

    var ul=$("#listViewVoteOptions");

    var ulli=ul.find("> li");

    if(ulli.length<=VOTE_OPTION_DEFAULT_NUMBER){
        return;
    }
     
    var firstEmptyFound=false;
    for(var i=0;i<ulli.length;++i){
        if($(ulli[i]).find("input").val()!=""){
            continue;
        }
            
        if(!firstEmptyFound){
            firstEmptyFound=true;
            continue;
        }

        ulli[i].remove();
        ul.listview("refresh");
        updateVoteOptionPlaceHolder();
        resizeLazyPanel();
        return;
    }
}


function clearVoteTopicInput(){
    $("#inputVoteTopicTopic").val("");
    var ulli=$("#listViewVoteOptions li");

    var toRemove=ulli.length>VOTE_OPTION_DEFAULT_NUMBER;
    if(toRemove>0){
        for(var i=0;i<toRemove;++i){
            $("#listViewVoteOptions >li:last-of-type").remove();
        }
    }

    $("#listViewVoteOptions input").val("");

    $("listViewVoteOptions").listview("refresh");
}

