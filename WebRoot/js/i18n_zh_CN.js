var INPUT_TALK_WAIT_TEXT="想说点什么？";
var INPUT_VOTE_TOPIC_WAIT_TEXT="投票主题(必填,请尽量简短)";
var TALK_SHOULD_NOT_EMPTY="您还什么都没说呢";
var BUTTON_TALK="发布！";
var TALK_SUCCESS_TEXT="发布成功！";
var INPUT_VOTE_TOPIC_OPTION_WAIT_TEXT="候选项%s";
var INPUT_VOTE_TOPIC_OPTION_LIMIT_TEXT="(最多%s个)";
var INPUT_VOTE_TOPIC_DESCRIPTION_WAIT_TEXT="有关投票的更多说明(选填)";

var TITLE_CONFIRM_CLEAR="清除内容?";
var CONTENT_CONFIRM_CLEAR="确定放弃已输入的内容?";

var TEXT_USER_QR_CODE_HINT=[];
TEXT_USER_QR_CODE_HINT[0]="您可点击图片";
var LINK_SAVE_QR_CODE_IMAGE="保存此二维码";
TEXT_USER_QR_CODE_HINT[1]="或者";
var LINK_SAVE_TEXT_IDENTITY="保存此ID文本信息";
TEXT_USER_QR_CODE_HINT[2]="以便下次登录。";

var TEXT_USER_QR_CODE_WARNING="保密此二维码和ID文本，否则他人可能恶意冒充您发言！";
var LABEL_LOAD_QR_CODE_IMAGE_OR_TEXT="加载之前保存的二维码图或ID文本登录";
var ERROR_CAN_NOT_PARSE_QR_CODE_IDENTITY="无法从二维码读取登录信息！二维码内容: %s";
var ERROR_CAN_NOT_DECODE_QR_CODE="无法解码二维码";
var LABEL_USER_NAME="您的名字（所有人可见）";
var LABEL_USER_ID_BYTES="您的唯一ID（仅本人可见不要泄露。或%s。%s）";
var LINK_ID_BYTES_DESCRIPTION="身份ID的说明";
var LINK_PASSWORD_LOGIN="使用密码登录";
var LABEL_USER_DESCRIPTION="您的个人描述（200字以内）";
var ERROR_USER_NAME_INVALID="'%s'不是合法的用户名。\n用户名中不可出现空白字符，也不可使用:\/#@等特殊字符";
var BUTTON_CHANGE_USER="切换/更新";
var DEFAULT_USER_DESCRIPTION="我就是随便说说";
var INFO_CHANGE_USER_SUCCESSED="切换/更新用户成功！";
var LABEL_NORMAL_LOGIN_HINT="您已成功登录";
var LABEL_RANDOM_LOGIN_HINT="您当前以随机用户登录。若希望下次仍能使用同一个账户，请您记录您的ID信息";
var LABEL_NOT_LOGIN_HINT="您当前尚未登录，可使用之前保存的二维码图片登录，或尝试随机登录";
var TOOL_TIP_LOGIN_QR_CODE_IMAGE="读取身份信息成功！是否立刻切换用户？";
var ERROR_IDENTIFY_TEXT_TOO_LARGE="身份标识文本过大，您是否选错了文件？";
var ERROR_CAN_NOT_DECODE_IDENTIFY_TEXT="无法从标识文本中读取您的身份信息：%s";
var ERROR_UNKNOWN_IDENTIFY_FILE_TYPE="未知的ID文件格式";
var ERROR_LOGIN_FAILED="登录失败";
var LABEL_PASSWORD="使用用户名+密码生成身份ID";
var TEXT_PASSWORD_HINT="密码仅用于和名字联合生成唯一的ID，省去记录ID的麻烦。密码本身并不是识别您身份的条件，也不会上传。请不要使用您的重要密码。";
var ERROR_DETAIL={};
ERROR_DETAIL["ID_CLASH"]="您的ID与其他在线用户冲突或在您的其他浏览器中打开，请尝试更换ID或关闭其他浏览器";
ERROR_DETAIL["NOT_LOGIN"]="您尚未登录，请尝试切换到用户页面登录或刷新页面";
ERROR_DETAIL["FILE_TOO_LARGE"]="上传文件过大";


var ERROR_VOTE_TOPIC_INVALID="投票主题无效，不可为空，也不可包含特殊字符";
var ERROR_VOTE_OPTION_EMPTY="投票选项不可为空";
var ERROR_MESSAGE_TOO_LONG="内容过长,不可超过%s个字符";

var ERROR_FILE_TOO_LARGE="分享文件大小 %s 超出服务器限制:%s";

var UNKNOWN_ERROR_MESSAGE="未知错误";

var TEXT_POPUP_UNREAD="收到%u条新微博，点击查看";
var TITLE_PAGE_SESSION="帐号";
var BUTTON_LOGIN_LATER="稍后";
var BUTTON_REFRESH="刷新";
var BUTTON_VIEW="查看";
var BUTTON_LATER="稍后";
var BUTTON_CANCEL="取消";
var BUTTON_CLEAR_AND_NOT_REMIND="确定,清除不需要提醒";
var BUTTON_POLL_SWITCH_CHAT="聊天";
var BUTTON_POLL_SWITCH_BLOG="微博";
var TITLE_USER_ID_BYTES_HINT="身份ID的用处";
var TEXT_USER_ID_BYTES_HINT=[];
TEXT_USER_ID_BYTES_HINT[0]="传统的社交网络，所有用户都登录到一个集中的服务器上";
TEXT_USER_ID_BYTES_HINT[1]="随便聊则完全不同，每个随便聊app都是一个服务器";
TEXT_USER_ID_BYTES_HINT[2]="身份ID即使在不同的服务器上也可以保证唯一性";
TEXT_USER_ID_BYTES_HINT[3]="当两个随便聊app进行信息同步时，通过身份ID识别，不同时间，不同地点登录的用户便连接成了一个大的网络！";

var BUTTON_VOTE="投票";
var BUTTON_LONG_TALK="盖楼";
var BUTTON_TOPIC="话题";
var BUTTON_MENTION="点名";
var BUTTON_UPLOAD="分享";
var BUTTON_PASSWORD_LOGIN_CANCEL="取消";

var LINK_BROADCAST="转发";
var LINK_COMMENT="评论";
var LINK_VIEW_AUTHOR="查看作者";
var LINK_DELETE="删除";
var LINK_BROADCAST_ORIGIN="原文转发";
var TEXT_BROADCAST="转发微博";

var TALK_SHARE_MESSAGE="分享文件";
var TALK_SHARE_IMAGE_MESSAGE="分享图片";

var BUTTON_CLEAR="清除";
var TITLE_TALK="发微博";
var TEXT_FILE_SHARE="下载";
var TEXT_REFERENCE_MESSAGE_PREVIEW="转发预览：";
var ERROR_REFERENCE_ID_BYTES="转发的格式有误";

var TEXT_FLIP_OPTION_MESSAGE="微博";
var TEXT_FLIP_OPTION_USER="用户";

var TEXT_FLIP_OPTION_VOTE_MULTI="多选";
var TEXT_FLIP_OPTION_VOTE_SINGLE="单选";

var TEXT_USER_QUERY_RESULT_ROW="创建时间:%s 他的微博:%s <br> 个人说明:%s";
var TEXT_IMAGE="图片";
var TEXT_DELETE_SUCCESS="删除成功";
var BUTTON_SEARCH="搜";

var TEXT_OPEN_VIDEO_PLAYER="直播";
var TEXT_OPEN_AUDIO_PLAYER="直播";

var TEXT_OPEN_VIDEO_BROWSER="直播2";
var TEXT_OPEN_AUDIO_BROWSER="直播2";

var TEXT_ORIGINAL_MESSAGE="  的原微博:";

var TEXT_AGREE="顶";
var TEXT_DISAGREE="踩";
var TEXT_REFERENCED="转发";
var TEXT_SEARCH_HELP="搜索 #话题# 可搜索相关话题";

var TEXT_REFERENCE_VOTE_TOPIC_PREVIEW=[];
TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[0]="参与";
TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[1]="的投票:";
TEXT_REFERENCE_VOTE_TOPIC_PREVIEW[2]="投票给";

var TEXT_VOTE_OF_ME_COMMENT='%s "%s",还有其他要说的？';

var TEXT_VOTE_TOPIC_NOT_VOTED="小投票,您尚未参与:";
var TEXT_VOTE_OPTION_NOT_VOTED="(您参与投票后可见)";
var TEXT_VOTE_TOPIC_VOTED_NUMBER="(已有%s人参与):";
var TEXT_VOTE_OPTION_VOTED_NUMBER="(%3.2f%%,%s票)";

var TEXT_VOTE_TOPIC_VOTED="小投票";

var TEXT_EXPAND="我来说两句"; 
var TEXT_COLLAPSE="收起"; 

var TEXT_VOTE_OF_ME_TITILE="投票给:%s";
var TEXT_VOTE_TOPIC_SENDER_INFO="%s发起投票:";
var TEXT_VOTE_INVISIBLE="匿名投票";
