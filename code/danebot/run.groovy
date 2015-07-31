@Grab('org.twitter4j:twitter4j-core:4.0.4')
@Grab('joda-time:joda-time:2.8.1')

import org.joda.time.DateTime
import org.joda.time.Interval

import twitter4j.Query
import twitter4j.RateLimitStatus
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory

class TwitterBot {

    private Twitter twitter = new TwitterFactory().instance

    private final String RATE_LIMIT_STATUS = '/application/rate_limit_status'
    private final String RATE_LIMIT_SEARCH = '/search/tweets'

    private final Long MIN_ACTION_DELAY = 5000L
    private Map lastRun = [:]

    List<Status> search(Map params) {
        checkRateLimit(RATE_LIMIT_SEARCH)

        Query query = new Query(params.searchText)
        query.resultType = Query.ResultType.recent

        if (params.sinceId) {
            query.sinceId(params.sinceId)
        }

        twitter.search(query).tweets
    }

    void tweet(String text) {
        StatusUpdate status = new StatusUpdate(text)

        try {
            twitter.updateStatus(status)
        }
        catch (TwitterException ex) {
        }
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

def run = { long delay, Closure closure ->
    Thread.start {
        Closure clonedClosure = closure.rehydrate(twitterBot, this, this)

        while (true) {
            clonedClosure(it)
            Thread.sleep delay
        }
    }
}

Number.metaClass.getMinutes = {
    60000L * delegate
}

def every = { long delay ->
    [run: { Closure closure -> run(delay, closure) }]
}

def binding = new Binding(every: every, minute: 60000L)
def shell = new GroovyShell(binding)
shell.evaluate(new File('command.groovy'))