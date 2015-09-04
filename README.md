# NearByTalk

a tiny social network **Server** running on mobile phone (Android)

social network is popular, but at some contries mobile data costs expensive ,
and there're very few free wifi so you can't enjoy it. NearByTalk is 
just designed for this situation : it's a tiny social network running 
on Android phone , nearby devices can access it ,talk through it ,share 
blog or pictures/music/movie or any files . All of this works **without**
connecting to internet.

it's just like "FireChat", but didn't require special client, any device with
wifi and browser can enjoy this. You can think it as a tiny Twitter web server
running Android.

NearByTalk not only works as a social network server, but also can be a CV 
publisher, advertisement board… all of this you just need a android phone.



#### how to achive this ? technically  ?

NearByTalk is combination of a web server and a DNS server, while running ,
it turns your Android phone as wifi hotspot , when some device connected, 
NearByTalk answer any DNS request to self server ,and redirect all web request 
to self server, so any web page request will guild client user to your server. 
that means NearByTalk is not limited to known people nearby. Even who don't 
know there's a server, There're possibilities they can reach your server .


#### currently completed Features:

* share simple text message just like twitter.
* retweet
* support @ others and # topic
* chat mode for realtime chat.
* share images/audio/video, can be viewed/played online or downloaded.
* did a little vote.
* a "chat build"


## code part


NearByTalk is written in java ,codebase contains two parts : 

NearByTalkShare :pure server implmentation ,can be running on PC to help easy  
                  developping.
Android adaptor : makes NearByTalkShare running on android .

server part based on servlet (jetty-7) , baking store use sqlite4java and 
sqlchiper (sqlite with encrypt plus CJK FTS ability, which comes from firefox), 
front web GUI based on Jquery Mobile.


I didn't choose the Android sqlite java binding, it's difficult to run on PC,
not suitable for pure server implementation. 


every client user has a public username (visible to everyone) and a private ID
(NearByTalk use SHA1 as ID, visible to user and server, not other users.),
every message also have a ID which is digest by content and user private ID 
(as well as timestamp, referened message ID) ,so you can verify every message

short term planned features:

long term planned features:

    even it didn't have a central server, user are global unique across different 
    server.  different server can exchange records .records will be merged 
    together if they're from a unique client (record exchange is not 
    implemented)



limitations:


