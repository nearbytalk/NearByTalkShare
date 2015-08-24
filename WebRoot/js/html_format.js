//some html element needs to be formated as inner html,declare here
var FORMAT_A_LINK_POPUP_USER_ID_BYTES_HINT='<a href="#popupUserIdBytesHint" \
                                           id="linkPopupUserIdBytesHint" \
                                           data-rel="popup" \
                                           data-transition="pop"></a>';

var FORMAT_A_LINK_POPUP_PASSWORD_LOGIN='<a href="javascript:onLinkPopupPasswordLoginClicked()" \
                                        id="linkPasswordLogin"></a>';

//nested reference text message
var FORMAT_REFERENCE_TEXT_MESSAGE='<br/> \
                            <div class="referenced-message" data-role="collapsible" \n \
                            data-collapsed="false" \n data-theme="a" \n \
                            data-content-theme="a" data-mini="true"> \n \
                            <h6><small class="user-name-text">%s</small><small>%s</small></h6> <p>%s</p> </div>';

var FORMAT_REFERENCE_IMAGE_SHARE='<br/> \
                                 <div align="center"> \
                                 <a class="fancybox" href="upload/%s" \
                                 title="%s"> \
                                 <img src="upload/%s" \
                                 class="message-share-img"></img></a></div>';

var FORMAT_REFERENCE_AUDIO_SHARE='<div><a href="javascript:popupAndPlay(false,\'%s\')">%s</a> \
				 <a href="upload/%s" target="_blank">%s</a> \
				 <a href="upload/%s" target="_blank" download="%s">%s</a></div>';

var FORMAT_REFERENCE_VIDEO_SHARE='<div><a href="javascript:popupAndPlay(true,\'%s\')">%s</a> \
				 <a href="upload/%s" target="_blank">%s</a> \
				 <a href="upload/%s" target="_blank" download="%s">%s</a></div>';


var FORMAT_NORMAL_FILE_SHARE='<a href="upload/%s" download="%s" target="_blank">%s</a>';

var FORMAT_LAZY_MESSAGE_ROW='<li id="%s" data-role="fieldcontain" class="lazyMessageList-li"> \n\
                                <div> \n\
                                    <img src="img/avatar.jpg" class="user-avatar-img"></img> \n\
                                    <a href="javascript:void(0)" style="padding-top:3px;" \n\
                                        onclick=\'onLinkPopupMessageMenuClicked(this)\'> \n\
                                    <h4 class="user-name-text">%s</h4></a> \n\
                                </div> \n\
                                <div class="message-info-bar">%s %s %s</div></li>';

var FORMAT_USER_QUERY_RESULT_ROW='<li data-role="fieldcontain"> \
                                 <img src="img/avatar.jpg" class="user-avatar-img"></img> \
                                 <h4 class="user-name-text">%s</h4> \
                                 <p class="shortMessage">%s</p></li>';

var FORMAT_REFERENCE_MESSAGE_PREVIEW='%s:<b class="user-name-text">%s</b>:%s';


var FORMAT_CROSS_INDEX_HYPER_LINK='<a class="topic" href="javascript:void(0)" onclick=\'javascript:onTopicClicked(this)\'>%s</a>';

var FORMAT_VOTE_OPTION_ROW='<li class="ui-field-contain"> \
                   <input data-clear-btn="true" type="text" onkeyup="onVoteOptionKeyUp(event)" data-mini="true"> \
                   </li>';

var FORMAT_PLAIN_TEXT_MESSAGE_HTML='<p class="%s plain-text lazyMessageList-li-p">%s</p>';

var FORMAT_VOTE_TOPIC_OPTIONS='<fieldset data-role="controlgroup" data-mini="true">\n \
                             <legend></legend> \n \
                              </fieldset>';

var FORMAT_VOTE_TOPIC_OPTION_MULTI_SELECTION_ROW=
            '<input name="%s" id="%s" type="checkbox" data-mini="true"> \n \
            <label for="%s">%s</label> \n';

var FORMAT_VOTE_TOPIC_OPTION_ROW=
            '<input name="%s" id="%s"  type="radio"> \n \
            <label for="%s">%s</label> \n';
var FORMAT_VOTE_TOPIC_RESULT_ROW='<input data-mini="true" disabled="disabled" \n \
                                 data-highlight="true" min="0" max="100" value="%s" type="range">';

var FORMAT_REFERENCE_VOTE_TOPIC_PREVIEW='%s<b class="user-name-text">%s</b>%s %s %s "%s"';

var FORMAT_VOTE_OPTION_VOTED_TEXT='<p>%s</p>';

var FORMAT_VOTE_OF_ME_VOTED='<div><p>%s</p></div>';

