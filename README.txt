NearByTalk

a tiny social network *Server* running on mobile phone (Android)

social network is popular, but in some contries mobile data cost is very high ,
and there're very few free wifi so you can't enjoy it. NearByTalk is 
just designed for this situation : it's a tiny social network server running 
on Android phone , any nearby devices can access it ,talk through it ,share 
blog or pictures/music/movie or any files .

it's just like "FireChat", but didn't require special client, any device with
wifi and browser can enjoy this. you can think it as a tiny Twitter web server
running on Android.


There're many social network, why NearByTalk born ?

1. as previously, some contries mobile data cost is very high. you don't want
   to turn on mobile data all the time.

2. FireChat can work without World Wild Web ,but needs client, plus, it's more 
                      like a realtime chat, not a social network. nethier lacks 
                      ability to control contents. baleful ones can flush screen.

you can use NearByTalk just as a social network server, or even your CV 
                      publisher, advertisement board .  all of them works without
                      World Wild Web.


Features:
    zero 

    share short text or long blog. provide a chat mode for realtime chating.
    share images/audio/video, can be viewed/played online

    even it didn't have a central server, user are global unique across different 
    server.  different server can exchange records .records will be merged 
    together if they're from a unique client (record exchange is not 
    implemented)



    


Technically 


NearByTalk is combination of a web server and a DNS server, while running ,
it turns your Android phone as a wifi hotspot , when some device connected, 
NearByTalk redirect all web request to self server, so any web page request 
will guild client user to your server. that means NearByTalk is not limited
to known people nearby. even people who don't know there's a server, they're
also possible to access your server .


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




