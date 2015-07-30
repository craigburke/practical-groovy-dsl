'Dane Cook sucks'.watch { tweetedStatus ->
    println "@${tweetedStatus.user.screenName} tweeted: ${tweetedStatus.text}"
    tweet 'Hey @DaneCook, somebody said this:' << tweetedStatus
    tweet "LEAVE DANE ALONE, @${tweetedStatus.user.screenName}!!!"
}