@Grab('org.twitter4j:twitter4j-core:4.0.4')
@Grab('joda-time:joda-time:2.8.1')

import groovy.transform.Synchronized
import org.joda.time.DateTime
import org.joda.time.Interval

import twitter4j.Query
import twitter4j.RateLimitStatus
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.UploadedMedia

class TwitterBot {

    private Twitter twitter = new TwitterFactory().instance

    private final String RATE_LIMIT_STATUS = '/application/rate_limit_status'
    private final String RATE_LIMIT_SEARCH = '/search/tweets'
    private final String RATE_LIMIT_FAVORITE = '/favorites/create'
    private final String RATE_LIMIT_RETWEET = '/statuses/retweet/:id'
    
    private final Long MIN_ACTION_DELAY = 5000L
    private Map lastRun = [:] 
    
    @Synchronized
    List<Status> search(Map params) {
        checkRateLimit(RATE_LIMIT_SEARCH)
        
        Query query = new Query(params.searchText)
        query.resultType = Query.ResultType.recent

        if (params.sinceId) {
            query.sinceId(params.sinceId)
        }

        twitter.search(query).tweets
    }
    
    @Synchronized
    void tweet(Map params = [:], String text) {
        StatusUpdate status = new StatusUpdate(text)

        if (params.pictures) {
            def uploadedPictures = []
            
            params.pictures.each { byte[] pictureData ->
                UploadedMedia picture = twitter.uploadMedia('', new ByteArrayInputStream(pictureData))
                uploadedPictures << picture.mediaId
            }

            status.mediaIds = uploadedPictures
        }
        
        try {
            twitter.updateStatus(status)
        }
        catch (TwitterException ex) { }
    }
    
    @Synchronized
    void favorite(Status tweet) {
        checkRateLimit(RATE_LIMIT_FAVORITE)
        twitter.createFavorite(tweet.id)
    }

    @Synchronized
    void retweet(Status tweet) { 
        checkRateLimit(RATE_LIMIT_RETWEET)
        twitter.retweetStatus(tweet.id)
    }
    
    private void checkRateLimit(String key) {
        DateTime now = new DateTime()

        if (lastRun[key]) {
            Interval lastRunInterval = new Interval(lastRun[key], new DateTime())
            if (lastRunInterval.toDurationMillis() < MIN_ACTION_DELAY) {
                return
            }
            
        }
        lastRun[key] = new DateTime()
        
        def statuses = twitter.rateLimitStatus
        
        RateLimitStatus checkStatus = statuses[RATE_LIMIT_STATUS]
        RateLimitStatus status = statuses[key]
        
        if (checkStatus.remaining && status.remaining) {
            return
        }
        
        Long checkStatusSleepTime = (checkStatus.remaining ? 0 : checkStatus.resetTimeInSeconds) * 1000L
        Long statusSleepTime = (status.remaining ? 0 : status.resetTimeInSeconds) * 1000L

        now = new DateTime()
        DateTime sleepUntilDate = new DateTime(Math.max(checkStatusSleepTime, statusSleepTime))
        
        if (now < sleepUntilDate) {
            Interval interval = new Interval(now, sleepUntilDate)
            long sleepTime = interval.toDurationMillis() + 1000L
            println "Rate Limit exceeded. Waiting for ${sleepTime / 1000L} seconds"
            Thread.sleep(sleepTime)
        }
    }
}

TwitterBot twitterBot = new TwitterBot()

String.metaClass.leftShift = { Status tweet ->
    "${delegate} http://twitter.com/${tweet.user.screenName}/status/${tweet.id}"
}

String.metaClass.watch = { Map params = [:], Closure closure ->
    String searchText = delegate
    
    Thread.start {
        Closure clonedClosure = closure.rehydrate(twitterBot, this, this)

        Long pollDelay = 5000L

        Map searchParams = [:]
        searchParams.searchText = searchText

        while (true) {
            def tweets = twitterBot.search(searchParams)
            tweets.each { clonedClosure(it) }
            searchParams.sinceId = tweets ? tweets.sort { it.id }.last().id : searchParams.sinceId
            Thread.sleep pollDelay
        }
    }
}

def shell = new GroovyShell()
shell.evaluate(new File('command.groovy'))