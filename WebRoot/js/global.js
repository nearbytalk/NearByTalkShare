"use strict";

var context = "/";

var VOTE_TOPIC_SENDER_NAME_KEY="VOTE_TOPIC_SENDER_NAME";

var VOTE_TOPIC_TOPIC_KEY="VOTE_TOPIC_TOPIC_KEY"; 

var VOTE_OPTION_KEY="VOTE_OPTION_KEY"; 

var VOTE_OF_ME_HIDDEN_CONTENT_KEY="VOTE_OF_ME_HIDDEN_CONTENT";

var GlobalInfo={
		sessionUser_ : null,
		
		sessionUser:function(sessionUserSet){
			if(sessionUserSet!=undefined && sessionUserSet!=null){
				this.sessionUser_=sessionUserSet;
			}
			
			return this.sessionUser_;
		},
		
		isLogined:function(){
			return this.sessionUser_!=null;
		}

};


var BROADCAST_LIMIT=200;
var SEARCH_USER_NAME_KEY='SEARCH_USER_NAME';
var SEARCH_TOPIC_KEY='SEARCH_TOPIC';
var SEARCH_TYPE_KEY='SEARCH_TYPE';

function randomSuffix(length) {
	var iteration = 0;
	var randomSuffix = "";
	var randomNumber;

	while (iteration < length) {
		randomNumber = (Math.floor((Math.random() * 100)) % 94) + 33;
		
			if ((randomNumber >= 33) && (randomNumber <= 47)) {
				continue;
			}
			if ((randomNumber >= 58) && (randomNumber <= 64)) {
				continue;
			}
			if ((randomNumber >= 91) && (randomNumber <= 96)) {
				continue;
			}
			if ((randomNumber >= 123) && (randomNumber <= 126)) {
				continue;
			}
		
		iteration++;
		randomSuffix += String.fromCharCode(randomNumber);
	}
	return randomSuffix;
}

/**
 * check error code and display it. 
 * if error is NOT_LOGIN, just try to do initLogin
 *
 * @errorCode 
 *
 */
function displayError(errorCode){

    if(errorCode=="NOT_LOGIN"){
        initLogin();
        return;
    }

     var detailMessage=ERROR_DETAIL[errorCode];

     if(detailMessage==null || detailMessage==undefined){
	  detailMessage=UNKNOWN_ERROR_MESSAGE;
     }

     toast(detailMessage);
}

function verifyIdBytes(idBytes){
	return /^[0-9a-z]{40}$/.test(idBytes);
}

var toast = function(msg) {
	$("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h3>"
					+ msg + "</h3></div>").css({
		display : "block",
		opacity : 0.90,
		position : "fixed",
		padding : "7px",
		"text-align" : "center",
		width : "270px",
		left : ($(window).width() - 284) / 2,
		top : $(window).height() / 2
	}).appendTo($.mobile.pageContainer).delay(1200).fadeOut(400, function() {
		$(this).remove();
	});
};

	  
var MAX_TOPIC_NUMBER=5;
var MAX_TOPIC_LENGTH=10;

function backAppend(arrayVar,toPushBack){

     var len=arrayVar.length;
     if(len==0){
	  arrayVar[0]=toPushBack;
	  return arrayVar;
     }

     if(toPushBack.isCrossIndex){
	  arrayVar[len]=toPushBack;
	  return arrayVar;
     }

     if(!arrayVar[len-1].isCrossIndex){

	  if(toPushBack.text==""){
	       return arrayVar;
	  }

	  arrayVar[len-1].text=arrayVar[len-1].text+toPushBack.text;
	  return arrayVar;
     }

     arrayVar[len]=toPushBack;
     return arrayVar;
}


function plainTextAddCrossIndex(plainText){
     //add cross index to plainText


     var jsonReg=/.*[\{\}\[\]:\"\\,\/].*/;

     var beginIndex=0;
     var endIndex=-1;

     var tokens=new Array();

     var crossIndexNumber=0;

     var first = true;
     do{
	  beginIndex=plainText.indexOf('#',beginIndex);
	  

	  if(beginIndex==-1){
	       //no more tokens

	       //just end

	       tokens=backAppend(tokens,{
		    isCrossIndex:false,
		    text:plainText.substring(endIndex+1)
	       });
	       break;

	  }


	  if(beginIndex!=endIndex+1){
	       //found startmark after some normal text

	       tokens=backAppend(tokens,{
		    isCrossIndex:false,
		    text:plainText.substring(endIndex+1,beginIndex)
	       });

	       endIndex=beginIndex-1;
	       continue;
	  }


	  //has crossIndex start mark

	  endIndex=plainText.indexOf('#',beginIndex+2);

	  if(endIndex==-1){
	       //no end mark
	       
	       
	       tokens=backAppend(tokens,{
		    isCrossIndex:false,
		    text:plainText.substring(beginIndex)
	       });

	       break;
	  }


	  if(endIndex-beginIndex>MAX_TOPIC_LENGTH+1 || jsonReg.test(plainText.substring(beginIndex+1,endIndex))){
	       //not a valid crossIndex
	       //
		    
	       //do not include end mark, since we need to search next valid one from that
	       tokens=backAppend(tokens,{
		    isCrossIndex:false,
		    text:plainText.substring(beginIndex,endIndex)
	       });

	       beginIndex=endIndex;
	       continue;
	  }


	  //valid cross index

	  tokens=backAppend(tokens,{
	       isCrossIndex:true,
	       text:plainText.substring(beginIndex,endIndex+1)
	  });

	  beginIndex=endIndex+1;
	  ++crossIndexNumber;
	  //MAX_TOPIC_NUMBER is defined in java Utility
     }while(crossIndexNumber<MAX_TOPIC_NUMBER);

     var forConvert=$("<div/>");

     var retHtml="";

     for(var idx in tokens){

	  var obj=tokens[idx];
	       
	  var escaped=forConvert.text(obj.text).html();
	  if(obj.isCrossIndex){

	       retHtml=retHtml.concat($.sprintf(FORMAT_CROSS_INDEX_HYPER_LINK,escaped));

	  }else{
	       retHtml=retHtml.concat(escaped);
	  }
     }

	
     return retHtml;

}

function createPlainText(message,depth,idSuffix,parentMessage){

    //create cross index

    var isShort=(message.plainText.length<=BROADCAST_LIMIT);

    var thisHtml=plainTextAddCrossIndex(message.plainText);

    var content=thisHtml.concat(
            recursiveAppend(message.referenceMessage,depth+1,idSuffix,message));


    var innerInner=(parentMessage==null?  
            //top level message is just inner html
            content:
            //child level message is formated as a inner block
            $.sprintf(FORMAT_REFERENCE_TEXT_MESSAGE,
                message.sender.userName,TEXT_ORIGINAL_MESSAGE,content));

            
    return $.sprintf(FORMAT_PLAIN_TEXT_MESSAGE_HTML,
            //style class 
            "lazyMessageList-"+isShort?"shortMessage":"longMessage",
            innerInner);
}

function createRefUnique(message,depth,idSuffix,parentMessage){

    //on windows host use \ as seperator
         var urlLink=message.fileName.replace('\\','/');
	 if(/\.(jpg|jpeg|bmp|png|gif)$/i.test(urlLink)){

	      var imageDescription=TEXT_IMAGE;
	      if(parentMessage!=null){
		   imageDescription=parentMessage.plainText.substr(
			     0,BROADCAST_LIMIT);
	      }

	      return $.sprintf(FORMAT_REFERENCE_IMAGE_SHARE,
			urlLink,imageDescription,urlLink);
	 }

	 var linkText=TEXT_FILE_SHARE;

	 var extensionPos=urlLink.indexOf('.');
	 if(extensionPos!=-1){
	      linkText=linkText+urlLink.substr(extensionPos);
	 }


	 if(/\.(mp3|m4a|oga|wav)$/i.test(urlLink)){
	      return $.sprintf(FORMAT_REFERENCE_AUDIO_SHARE,
			urlLink,TEXT_OPEN_AUDIO_PLAYER,
			urlLink,TEXT_OPEN_AUDIO_BROWSER,
			urlLink,linkText,linkText);
	 }

	 if(/\.(flv|m4v|avi|ogg|mpg|mpeg|ogv|webm|mp4)$/i.test(urlLink)){

	      return $.sprintf(FORMAT_REFERENCE_VIDEO_SHARE,
			urlLink,TEXT_OPEN_VIDEO_PLAYER,
			urlLink,TEXT_OPEN_VIDEO_BROWSER,
			urlLink,linkText,linkText);
	 }

      
	 return $.sprintf(FORMAT_NORMAL_FILE_SHARE,urlLink,linkText,linkText);
}

function onVoteOptionRadioChecked(onChangeEvent){

    var inputObj=$(onChangeEvent.target);

    var voteTopicFieldSet=inputObj.closest("fieldset");

    var tempIdForClear=randomSuffix(6);

    voteTopicFieldSet.attr("id",tempIdForClear);

    var bareIdBytes=voteTopicFieldSet.jqmData(VOTE_TOPIC_ID_BYTES_KEY);

    var voteTopic=voteTopicFieldSet.jqmData(VOTE_TOPIC_TOPIC_KEY);

    var thisOption=inputObj.jqmData(VOTE_OPTION_KEY);

    var voteTopicSenderName=voteTopicFieldSet.jqmData(VOTE_TOPIC_SENDER_NAME_KEY);

    var options=new Array(); 
    if (onChangeEvent.target.type=="radio") {
        //single selection
        options[0]=thisOption; 
    }else {
        var allSelection=voteTopicFieldSet.find("input"); 
        for(var i =0; i<allSelection.length; ++i){
            if(allSelection[i].checked){
                options.push($(allSelection[i]).jqmData(VOTE_OPTION_KEY)); 
            }
        }
    }

    var joinedOptions=options.join(); 

    $("#textReferenceMessagePreview").html(
            $.sprintf(FORMAT_REFERENCE_VOTE_TOPIC_PREVIEW,
	      TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[0],
	      voteTopicSenderName,
          TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[1],voteTopic,
          TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[2],joinedOptions)).show();

    //attach referenceIdBytes to input 
    $("#inputTalk").jqmData(REFERENCE_ID_BYTES_KEY,bareIdBytes);

    //attach which VoteTopic action comes from ,button clear will 
    //remove selection if user gives up talk action
    $("#inputTalk").jqmData(VOTE_TOPIC_FIELD_SET_TEMP_ID_KEY,tempIdForClear);


    $("#inputTalk").jqmData(VOTE_OF_ME_OPTIONS_KEY,options);
    $("#inputTalk").jqmData(REFERENCE_ID_BYTES_KEY,bareIdBytes);

    presetInputTalk("");

    $("#inputTalk").attr("placeholder",
            $.sprintf(TEXT_VOTE_OF_ME_COMMENT,
                TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[2],joinedOptions));
}

function voteTopicTitleInfo(message,parentMessage){
    var prefix= (parentMessage==null?"":
        $.sprintf(TEXT_VOTE_TOPIC_SENDER_INFO,
            message.sender.userName));

    if(prefix==""){ 
        prefix+= (message.isProtectedProxy?
                TEXT_VOTE_TOPIC_NOT_VOTED:
                TEXT_VOTE_TOPIC_VOTED);
    }

    if(message.isProtectedProxy){
        return prefix+message.voteTopic
            +(message.multiSelection?
                    "("+TEXT_FLIP_OPTION_VOTE_MULTI+")"
                    :"");
    }else{
        return prefix+message.voteTopic;
    }
}

function createVoteTopicNoneVoted(message,outerContainer,parentMessage){

    var title=voteTopicTitleInfo(message,parentMessage);
    // user not voted 
    outerContainer.find("legend").text(title);

    outerContainer.jqmData(VOTE_TOPIC_ID_BYTES_KEY,message.idBytes);
    outerContainer.jqmData(VOTE_TOPIC_SENDER_NAME_KEY,message.sender.userName);

    outerContainer.jqmData(VOTE_TOPIC_TOPIC_KEY,message.voteTopic); 

    
    var radioGroupName=randomSuffix(6);

    var options=message.options;

    for(var i=0;i<options.length;++i){
        var randomName=randomSuffix(6);

        var optionHtml;
        var optionText=options[i]+TEXT_VOTE_OPTION_NOT_VOTED;

        if(message.multiSelection){
            optionHtml= $.sprintf(FORMAT_VOTE_TOPIC_OPTION_MULTI_SELECTION_ROW,
                    randomName,randomName,randomName,optionText);
        }else{
            optionHtml= $.sprintf(FORMAT_VOTE_TOPIC_OPTION_ROW,
                    radioGroupName,randomName,randomName,optionText);
        }

        outerContainer.find("*:last-child").after(optionHtml).enhanceWithin();
        var inputs=outerContainer.find("input"); 
        $(inputs[inputs.length-1]).jqmData(VOTE_OPTION_KEY,options[i]); 
    }

    outerContainer.on("change","input",onVoteOptionRadioChecked);

    outerContainer.find("*:last-child").after(message.description);

    return outerContainer;
}
function createVoteTopicVoted(message,outerContainer,parentMessage,firstHistory){

    
    //user voted
    var results=message.results;


    var totalNumber=0;

    //calc percent
    for(var key in results){
        if(results.hasOwnProperty(key)){
            totalNumber+=results[key];
        }
    }
    var title=voteTopicTitleInfo(message,parentMessage)
            +
            $.sprintf(TEXT_VOTE_TOPIC_VOTED_NUMBER,totalNumber);

    outerContainer.find("legend").text(title);

    for(var key in results){
        if(results.hasOwnProperty(key)){


            var thisNumber=results[key];
            var percent=
                (totalNumber==0?0:
                thisNumber/totalNumber*100);

            var optionText=$.sprintf(
                     FORMAT_VOTE_OPTION_VOTED_TEXT,
                     key+
                     $.sprintf(TEXT_VOTE_OPTION_VOTED_NUMBER,percent,thisNumber));

            outerContainer.find("*:last-child").after(optionText);

            var resultRow=$($.sprintf(FORMAT_VOTE_TOPIC_RESULT_ROW,percent));
            outerContainer.find("*:last-child").after(resultRow);
        }
    }

    outerContainer.find("*:last-child").after(message.description);

    //update exists VoteOfMe with hidden options to visible

    if (!firstHistory) {
        //this is new message polled 
        showHiddenVoteOfMeOptions(message.idBytes);
    }
    
    return outerContainer;
}

    
function showHiddenVoteOfMeOptions(voteTopicIdBytes){

    var selector=$.mobile.ns+VOTE_OF_ME_HIDDEN_CONTENT_KEY;
    var voteOfMeList=$("#lazyMessageList > li > div > div:data("+selector+")");

    for(var i =0;i<voteOfMeList.length;++i){ 
        var voteOfMeObj=$(voteOfMeList[i]);
        var voteTopicDom=voteOfMeObj.find(" > fieldset");
        var idBytesToTest=voteTopicDom.jqmData(VOTE_TOPIC_ID_BYTES_KEY);
        if (voteTopicIdBytes==idBytesToTest) {
            //should update 
            
            var hiddenContent=voteOfMeObj.jqmData(
                    VOTE_OF_ME_HIDDEN_CONTENT_KEY);
            voteOfMeObj.find("> p").text(hiddenContent);
        }
    }
}

function createVoteTopic(message,depth,idSuffix,parentMessage,firstHistory){


    var outerContainer=
        $(FORMAT_VOTE_TOPIC_OPTIONS//this text can be extended
                    //actual voteTopic is stored 
                    //at jqmData VOTE_TOPIC_TOPIC_KEY
                    );

    if(parentMessage!=null){
        outerContainer.css("margin-left","30px");
    }else{
        outerContainer.css("margin-top","15px");
    }

        return message.isProtectedProxy?
            createVoteTopicNoneVoted(message,outerContainer,parentMessage):
            createVoteTopicVoted(message,outerContainer,parentMessage,firstHistory);
    
}


function createVoteOfMe(message,depth,idSuffix,parentMessage,firstHistory){

    var innerDom=createVoteTopic(message.referenceMessage,depth+1,idSuffix,message,firstHistory);

    innerDom.addClass("tnm-wrap-vote-topic");

    var options=message.options;

    var content;
    var hiddenContent=false;

    if (options==null) {
        //vote always invisible
            content = TEXT_VOTE_INVISIBLE;
        
    }else{
        content=$.sprintf(TEXT_VOTE_OF_ME_TITILE,
                options.join(','))+
            (isEmptyString(message.comment)?
             "":
             (","+message.comment));

        if (message.isProtectedProxy) {
            //VISIBLE_AFTER_VOTE,store this info
            //to data, then restore back after
            //user voted

            hiddenContent=true;
        }
    }

    var html=$.sprintf(FORMAT_VOTE_OF_ME_VOTED,
            hiddenContent?
            TEXT_VOTE_OPTION_NOT_VOTED
            :content);

    var voteOfMeDom=$(html);

    if (hiddenContent) {
        //when user voted on topic ,restore this information
        voteOfMeDom.jqmData(VOTE_OF_ME_HIDDEN_CONTENT_KEY,content);
    }

    voteOfMeDom.find("*:last-child").after(innerDom);

    return voteOfMeDom;
}

function createChatBuild(message,depth,idSuffix,parentMessage){
}

function recursiveAppend(message,depth, idSuffix, parentMessage,firstHistory){
     if(message==null || message==undefined || depth>10){
	  return "";
     }

     if(message.messageType=="PLAIN_TEXT"){

         return createPlainText(message,depth,idSuffix,parentMessage);
	  
     }
     
     if(message.messageType=="REF_UNIQUE"){
	       
         return createRefUnique(message,depth,idSuffix,parentMessage);
         
     }

     if(message.messageType=="VOTE_TOPIC"){
         return createVoteTopic(message,depth,idSuffix,parentMessage,firstHistory);
     }

     if(message.messageType=="VOTE_OF_ME"){
         return createVoteOfMe(message,depth,idSuffix,parentMessage,firstHistory);
     }

     if(message.messageType=="CHAT_BUILD"){
         return createChatBuild(message,depth,idSuffix,parentMessage);
     }

     //TODO other type

}

function createMessageBlogHtml(thisMessage,idSuffix,firstHistory){
    var innerDom=recursiveAppend(thisMessage,0,idSuffix,null,firstHistory);

    

    var isBroadcast=false;

    var isMyTalk=(thisMessage.sender.userName==
              GlobalInfo.sessionUser().userName);

    var innerMostReferenceIdBytes=thisMessage.idBytes;

    if(thisMessage.messageType=='PLAIN_TEXT' &&
            thisMessage.referenceMessage !=null && 
            thisMessage.referenceMessage.messageType!='REF_UNIQUE'){
        isBroadcast=true;

        innerMostReferenceIdBytes=thisMessage.referenceMessage.idBytes;
    }else{
        //TODO other message type inner most idbytes
    }

    var toAppend=$.sprintf(FORMAT_LAZY_MESSAGE_ROW,
            //element id
            createSuffixedId(thisMessage.idBytes,idSuffix),
            //username
            thisMessage.sender.userName,
	    TEXT_AGREE+thisMessage.agreeCounter,
	    TEXT_DISAGREE+thisMessage.disagreeCounter,
	    TEXT_REFERENCED+thisMessage.referencedCounter);

    var toAppendDom=$(toAppend);

    toAppendDom.find("div>a").after(innerDom);

    return {
        "dom":toAppendDom,
        "isBroadcast":isBroadcast,
        "innerMostReferenceIdBytes":innerMostReferenceIdBytes,
        "isMyTalk":isMyTalk
    };
}

function createSuffixedId(id,suffix){
    return id+suffix;
}

function extractIdBytesFromId(id){
    return id.slice(0,40);
}


function extractUrlAndExtension(name){

     var url=window.location.origin+'/upload/'+name;
    
     var extension=url.substring(url.lastIndexOf('.')+1);

     return {
	  "url":url,
	   "extension":extension
     };

}

function onTopicClicked(ele){
     anyPageSearchTopic($(ele).text());
}

function anyPageSearchTopic(topicWithMark){

     var toSearch=topicWithMark;

     var currentPage=$.mobile.activePage.attr('id');
     if(currentPage!='pageSearch'){
	  //we need jump across page
	  $('#pageSearch').jqmData(SEARCH_TYPE_KEY,SEARCH_TOPIC_KEY);
	  $('#pageSearch').jqmData(SEARCH_TOPIC_KEY,toSearch);
	  $.mobile.changePage("#pageSearch");
	  return;
     }

     //already at search page
     $("#inputSearch").val(topicWithMark);

     //only search inner text between start/end # mark
     sendQueryTopic(toSearch);

}

function sendQueryTopic(toSearchWithoutMark){

     var extraQuery=null;

     if(toSearchWithoutMark.length<MAX_TOPIC_LENGTH){
	  // may have similar topic
	  extraQuery=function(){
	      sendSearchQuery("message",{
		   searchType:"TOPIC_FUZZY",
	      	   keywords:toSearchWithoutMark
	      });

	 };
     }

     sendSearchQuery("message",{
	      searchType:"TOPIC_EXACTLY",
	      keywords:toSearchWithoutMark
	 },extraQuery);
	 
}

function popupAndPlay(isVideo,name){

     if(isVideo){
	  $("#popupVideoPlayer").popup("open");
     }else{

	  $("#popupAudioPlayer").popup("open");
     }


     var urlAndExtension=extractUrlAndExtension(name);

     var jqueryEle=isVideo?
	  $("#video_jplayer"):
	  $("#audio_jplayer");

     var mediaArgs={};

        var key=urlAndExtension.extension;

        if(isVideo ){
	     if(urlAndExtension.extension=='webm'){
		  key='webmv';
	     }else if(urlAndExtension.extension=="mp4"){
		  key="m4v";
	     }else if(urlAndExtension.extension=="ogg"){
		  key="ogv";
	     }
        }else{
	     if(urlAndExtension.extension=='ogg'){
		  key='oga';}
	}
         
        mediaArgs[key]=urlAndExtension.url;
     
	     
	
	if(isVideo){


     var selectSolution="html,flash";
     if(urlAndExtension.extension=="flv"){
	  selectSolution="flash,html";
     }

     $("#video_jplayer").jPlayer({

	  ready: function () {
		      $(this).jPlayer("setMedia", mediaArgs);
		      $(this).jPlayer("option",{fullScreen:true});
		      $(this).jPlayer("play");
		 },
	  swfPath: "/js",
	  solution:selectSolution,
	  supplied: key,
	  cssSelectorAncestor: "#video_jplayer_container",
	  size: {
	  cssClass: "jp-video-full"
	  },
	  smoothPlayBar: true,
	  keyEnabled: true
     });


	}else{

	     var myCirclePlayer = new CirclePlayer("#audio_jplayer",
		       mediaArgs, 
		       {
			    ready:function(){
				       $("#audio_jplayer").jPlayer("setMedia",mediaArgs);
				       $("#audio_jplayer").jPlayer("play");

				       setTimeout(function(){
					    $("#audio_jplayer").jPlayer("play");
				       },100);


				  },
		 supplied:urlAndExtension.extension,
		 solution:"flash,html",
		 swfPath: "/js",
		 wmode: "window",
		 cssSelectorAncestor: "#audio_jplayer_container",
		 keyEnabled: true
		       });


	}
}

function isEmptyString(toTest){

    return /^[\s\t]*$/.test(toTest);
}


