// Monitor twitter for negative tweets
'Dane Cook sucks'.watch { tweetedStatus ->
    tweet 'Hey @DaneCook, somebody said this:' << tweetedStatus
    tweet "LEAVE DANE ALONE, @${tweetedStatus.user.screenName}!!!"
}

// Reassure Dane every 10 minutes
every 10.minutes run {
	tweet 'Reminder: you are absolutely hilarious @DaneCook and everyone loves you!'
}