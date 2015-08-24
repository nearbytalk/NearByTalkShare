"use strict";

$(document).delegate("#pageSession", "pagecreate", function() {
	$("#imgAvatar").css("width", "80%");

	$("#textUserQrCodeWarning").text(TEXT_USER_QR_CODE_WARNING);

	$("#labelLoadQrCodeImageOrText").text(LABEL_LOAD_QR_CODE_IMAGE_OR_TEXT);


    $("#labelUserName").text(LABEL_USER_NAME);

    $("#labelUserIdBytes").html(
        $.sprintf(LABEL_USER_ID_BYTES,
            FORMAT_A_LINK_POPUP_PASSWORD_LOGIN,
            FORMAT_A_LINK_POPUP_USER_ID_BYTES_HINT));

    $("#linkPopupUserIdBytesHint").text(LINK_ID_BYTES_DESCRIPTION);

    $("#linkPasswordLogin").text(LINK_PASSWORD_LOGIN);

    $("#labelUserDescription").text(LABEL_USER_DESCRIPTION);

    $("#buttonChangeUser").text(BUTTON_CHANGE_USER);

    $("#buttonPasswordLoginCancel").text(BUTTON_PASSWORD_LOGIN_CANCEL);

    $("#toolTipLoginQrCodeImage").text(TOOL_TIP_LOGIN_QR_CODE_IMAGE);

    $("#labelPassword").text(LABEL_PASSWORD);

    $("#titlePageSession").text(TITLE_PAGE_SESSION);

    $("#buttonLoginLater").text(BUTTON_LOGIN_LATER);

    $("#textPasswordHint").text(TEXT_PASSWORD_HINT);

    $("#titleUserIdBytesHint").text(TITLE_USER_ID_BYTES_HINT);

    $("#popupLoginQrCodeImage").popup({transition:"pop"});

    $("#popupPasswordLogin").popup({transition:"pop"});

    for(var i in TEXT_USER_QR_CODE_HINT){
        $("#labelUserQrCodeHint"+i).text(TEXT_USER_QR_CODE_HINT[i]);
    }

    for(var i in TEXT_USER_ID_BYTES_HINT){
	 $("#textUserIdBytesHint"+i).text(TEXT_USER_ID_BYTES_HINT[i]);
    }

    $("#linkSaveQrCodeImage").text(LINK_SAVE_QR_CODE_IMAGE);

    $("#linkSaveTextIdentity").text(LINK_SAVE_TEXT_IDENTITY);

    $("#inputTempUserName").bind("input propertychange",onPasswordChanged);

    $("#inputPassword").bind("input propertychange",onPasswordChanged);
});

function verifyUserName(userName) {
	// must not null ,and not empty
	if (!userName) {
		return false;
	}

	// should not contains blank
	if (/^.*\s+.*$/.test(userName)) {
		return false;
	}

	//do not allow :/\#@ in userName
	if(/[:\/#@]+/.test(userName)){
	     return false;
	}

	return true;
}



function makeupNearByTalkUrl(idBytes,userName,description){

        return JSON.stringify({
                userName:userName,
                idBytes:idBytes,
        description:description});
}

function base64ArrayBuffer(arrayBuffer) {
     var base64 = '';
     var encodings = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

     var bytes = arrayBuffer;
     var byteLength = bytes.byteLength;
     var byteRemainder = byteLength % 3;
     var mainLength = byteLength - byteRemainder;

     var a, b, c, d;
     var chunk;

     // Main loop deals with bytes in chunks of 3
     for (var i = 0; i < mainLength; i = i + 3) {
	  // Combine the three bytes into a single integer
	  chunk = 0xFF&&(bytes[i] << 16) | 0xFF && (bytes[i + 1] << 8) | bytes[i + 2];

	  // Use bitmasks to extract 6-bit segments from the triplet
	  a = (chunk & 16515072) >> 18; // 16515072 = (2^6 - 1) << 18
	  b = (chunk & 258048) >> 12; // 258048 = (2^6 - 1) << 12
	  c = (chunk & 4032) >> 6; // 4032 = (2^6 - 1) << 6
	  d = chunk & 63; // 63 = 2^6 - 1

	  // Convert the raw binary segments to the appropriate ASCII encoding
	  base64 += encodings[a] + encodings[b] + encodings[c] + encodings[d];
     }

     // Deal with the remaining bytes and padding
     if (byteRemainder == 1) {
	  chunk = bytes[mainLength];

	  a = (chunk & 252) >> 2; // 252 = (2^6 - 1) << 2

	  // Set the 4 least significant bits to zero
	  b = (chunk & 3) << 4; // 3 = 2^2 - 1

	  base64 += encodings[a] + encodings[b] + '==';
     } else if (byteRemainder == 2) {
	  chunk = (bytes[mainLength] << 8) | bytes[mainLength + 1];

	  a = (chunk & 64512) >> 10; // 64512 = (2^6 - 1) << 10
	  b = (chunk & 1008) >> 4; // 1008 = (2^6 - 1) << 4

	  // Set the 2 least significant bits to zero
	  c = (chunk & 15) << 2; // 15 = 2^4 - 1

	  base64 += encodings[a] + encodings[b] + encodings[c] + '=';
     }
     return base64;
}


function generateTextIdentityDownload(nearByTalkUrl){
     var onlyASCII=unescape(encodeURIComponent(nearByTalkUrl));

     var byteChars=new Array();
     byteChars.length=onlyASCII.length;

     for(var i=0;i<onlyASCII.length;++i){
	  byteChars[i]=onlyASCII.charCodeAt(i);
     }

     var hrefUrl="data:text/plain;charset=UTF-8;base64,";
     $("#linkSaveTextIdentity").prop("href",hrefUrl+base64ArrayBuffer(byteChars));
}

function parseQrCode(jsonText){

     var deserialize;
     try{
	  deserialize=$.parseJSON(jsonText);
     }catch(e){
	  return false;
     }

     if(deserialize.userName== null ||
        deserialize.idBytes == null||
        deserialize.description ==null){
             return false;
     }

     if(!verifyIdBytes(deserialize.idBytes)){
             return false;
     }

     if(!verifyUserName(deserialize.userName)){
             return false;
     }

     if(!verifyDescription(deserialize.description)){
             return false;
     }

     $("#inputUserName").val(deserialize.userName);
     $("#inputUserIdBytes").val(deserialize.idBytes);
     $("#inputUserDescription").val(deserialize.description);
     //TODO resize description

     return true;
}

function fillQrCode(toQrCode) {

     var canvasEle = document.getElementById("canvasQrCodeImage");

     var onlyASCII=unescape(encodeURIComponent(toQrCode));

     //draw qrcode
     var qr = new QRCodeDecode();

     var bit8Mode=4;

     var hLevelEc=2;

	var version = qr.getVersionFromLength(
                    hLevelEc, bit8Mode, onlyASCII.length);

    var pixelModule=Math.floor(256/qr.nModulesFromVersion(version));

     qr.encodeToCanvas(bit8Mode, onlyASCII, version, hLevelEc, 
                     pixelModule, canvasEle, [1,1,1], [0,0,0]);


     var finalWidth=canvasEle.width;

     var offset=(finalWidth-48)/2;

     var ctx=canvasEle.getContext('2d');

     var logoImg=$("#imgLogo");

     var overlayDraw=function(){
         //draw canvas to hidden div
         ctx.drawImage(logoImg.get(0),offset,offset);

         //convert to img data url

         var dataURL=canvasEle.toDataURL();


         $("#imgQrCode").prop("src",dataURL);

         $("#linkSaveQrCodeImage").prop("href",dataURL);

         $("#linkQrCodeImage").prop("href",dataURL);
     }

     logoImg.one('load',overlayDraw).each(
             function(){
                 if(this.complete){
                     overlayDraw();
                 }
             }
             );
}

function fillSessionField(){

     var sessionUser=GlobalInfo.sessionUser();

	$("#inputUserName").val(sessionUser.userName);
	$("#inputUserIdBytes").val(sessionUser.idBytes);
	$("#inputUserDescription").val(sessionUser.description);

    var nearByTalkUrl=makeupNearByTalkUrl(sessionUser.idBytes,
	       sessionUser.userName,sessionUser.description);

	fillQrCode(nearByTalkUrl);

	generateTextIdentityDownload(nearByTalkUrl);

    $("#labelLoginHint").text(
                    GlobalInfo.sessionUser().randomUser?
                    LABEL_RANDOM_LOGIN_HINT:LABEL_NORMAL_LOGIN_HINT);

              
    //if login success ,clear identify file field ,for next select
    $("#buttonLoadQrCodeImageOrText").val("");
}

function popupLoadIdentifySuccessDialog(){
     $('html,body').stop().animate({scrollTop:0},500 ,
	       function(){
		    $("#popupLoginQrCodeImage").popup("open");
	       });
}

function loadQrCodeImage(fileEvent){
     var canvas = document.getElementById("canvasQrCodeImage");

     canvas_loader(fileEvent ,canvas, function(){
	  var qr = new QRCodeDecode();

	  var ctx = canvas.getContext("2d");

	  var imagedata = ctx.getImageData(0, 0, canvas.width, canvas.height);

      try{
              var decodedString = qr.decodeImageData(imagedata, canvas.width, canvas.height);

              var decoded=decodeURIComponent(escape(decodedString));

              var successParsed=parseQrCode(decoded);

              if(successParsed){
		   popupLoadIdentifySuccessDialog();
		   return;
              }


              toast($.vsprintf(ERROR_CAN_NOT_PARSE_QR_CODE_IDENTITY,[decoded]));
              //must clear file select 

              $("#buttonLoadQrCodeImageOrText").val("");


      }catch(e){
              toast(ERROR_CAN_NOT_DECODE_QR_CODE);
              $("#buttonLoadQrCodeImageOrText").val("");
      }

     });



}

function loadJsonText(file){

     if(file.size>10240){
	  toast(ERROR_IDENTIFY_TEXT_TOO_LARGE);
	  return;
     }

     var reader = new FileReader();

     reader.onload = ( function(e) {

	  var jsonText=e.target.result;

	  var parseSucces=parseQrCode(jsonText);

	  if(parseSucces){
	       popupLoadIdentifySuccessDialog();
	       return;
	  }

	  toast($.vsprintf(ERROR_CAN_NOT_DECODE_IDENTIFY_TEXT,[jsonText]));
     });

     // Read in the image file as a data URL.
     reader.readAsText(file,"UTF-8");

}

function onButtonLoadQrCodeImageOrTextClicked(fileEvent){

     var selectedFiles=fileEvent.target.files.length;

     if(selectedFiles==0){
	  return;
     }

     var file=fileEvent.target.files[0];

     if(/^image/.test(file.type)){
	  loadQrCodeImage(fileEvent);
     }else if(/^text\/plain/.test(file.type)){
	  loadJsonText(file);
     }else{
	  toast($.vsprintf(ERROR_UNKNOWN_IDENTIFY_FILE_TYPE,[file.name]));
     }

}


/**
 * try to login into server . it use following logic :
 * 1 try to receive session info from server, update local info if already had.
 * 2 no server session found, try to login use local info.(server restart, or cross server) 
 * 3 use random login
 */
function initLogin() {
	// first time page loaded login
	
	//do background login
	$.ajax({
		url : "/GlobalInfoServlet",
		type : "POST",
		success : function(jsonResult){

            if(!jsonResult.success){
                displayError(jsonResult.detail);
                return;
            }
		
			//can not determined if still in session
			if(jsonResult.detail.sessionUser != null){

                //already has session in server, fill back information

				GlobalInfo.sessionUser(jsonResult.detail.sessionUser);
				
				if(GlobalInfo.sessionUser().randomUser){
					toast("login as random user:"+
							GlobalInfo.sessionUser().userName+
							",\n idBytes:"+GlobalInfo.sessionUser().idBytes);
				}

				GlobalInfo.userDescriptionMaxLength=jsonResult.detail.userDescriptionMaxLength;
				GlobalInfo.fileUploadLimitByte=jsonResult.detail.fileUploadLimitByte;
				
				fillSessionField();
				return;
			}else if(GlobalInfo.sessionUser()!=null){
                //no session in server side,
                //try to use last info stored in local variable
                //check fields first
                var localInfo=GlobalInfo.sessionUser();
                if(verifyIdBytes(localInfo.idBytes)&&
                   verifyUserName(localInfo.userName)&&
                   verifyDescription(localInfo.description)){
                    
                    userLogin(localInfo,true);
                    return;
                }

                //try new random login
            }

			// generate random user ,and try to login
			var randomUserName = "guest_" + randomSuffix(6);

			$("#inputUserName").val(randomUserName);

			var sha1 = $.sha1(randomUserName+randomSuffix(6));

			$("#inputUserIdBytes").val(sha1);

            var description=DEFAULT_USER_DESCRIPTION;

            $("#inputUserDescription").val(description);

			userLogin({userName:randomUserName, 
                    idBytes:sha1 ,
                    description:description},
                    true);
			
			},
		dataType : "json"
	});


	
}


function receiveLoginResult(jsonResult,silent,firstTime){
	
	if(jsonResult.success){
		GlobalInfo.sessionUser(jsonResult.detail);
		fillSessionField();
		if(!silent){
			toast(INFO_CHANGE_USER_SUCCESSED);
		}
	}else{
		if(!silent){
			toast(ERROR_LOGIN_FAILED);
		}
		     
		$("#labelLoginHint").text(firstTime?LABEL_NOT_LOGIN_HINT:
			  ERROR_DETAIL[jsonResult.detail]);
	}
}

function userLogin(clientUserInfo,silent) {
	
	$.ajax({
		url : "/LoginServlet",
		beforeSend : function() {
			if(silent!=undefined && silent){

			$.mobile.loading('show', {
			     theme: "a",
			     text: "loading...",
			     textonly: true,
			     textVisible: true
			});

			}
		},
		complete : function() {
			if(silent!=undefined && silent){
			      $.mobile.loading( "hide" );
			}
		},
		type : "POST",
		success : function(jsonResult){
			receiveLoginResult(jsonResult,silent);
			},
		dataType : "json",
		data : JSON.stringify(clientUserInfo),
		
		error : function(){
			toast("network error");
		}
		
	});

}

function verifyDescription(description){
	if (!description) {
		return false;
	}

    return description.length<=200;
}

function onButtonChangeUserClicked() {
	
	var userName = $.trim($("#inputUserName").val());

	if (!verifyUserName(userName)) {

		toast($.vsprintf(ERROR_USER_NAME_INVALID,[userName]));

		return;
	}
	
	var idBytes=$.trim($("#inputUserIdBytes").val());
	
	if (!verifyIdBytes(idBytes)){
		
		toast("idbytes is not valid");
		return;
	}

    var description=$("#inputUserDescription").val();

	userLogin({userName:userName,
            idBytes:idBytes,
            description:description},false);

}

function onPasswordChanged(){


    var userName=$("#inputTempUserName").val();

    if(!verifyUserName(userName)){
        toast($.sprintf(ERROR_USER_NAME_INVALID,userName));
    
        $("#buttonPasswordLoginOk").button("disable");
        return;
    }
    
    //only enable ok when id bytes is valid
    $("#buttonPasswordLoginOk").button("enable");

    var password=$("#inputPassword").val();

    $("#inputGeneratedIdBytes").val($.sha1(userName+password));
}

function onLinkPopupPasswordLoginClicked(){
    $("#inputTempUserName").val($("#inputUserName").val());
    //do not allow invalid id bytes (when dialog popup)
    $("#buttonPasswordLoginOk").button("disable");
    $("#popupPasswordLogin").popup("open");
}


function onButtonPasswordLoginOkClicked(){
    //copy temp value back to session page

    $("#inputUserName").val($("#inputTempUserName").val());
    $("#inputUserIdBytes").val($("#inputGeneratedIdBytes").val());
    $("#popupPasswordLogin").popup("close");
    onButtonChangeUserClicked();
}

