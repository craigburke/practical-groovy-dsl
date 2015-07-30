@Grab("org.grails:grails-datastore-gorm-hibernate4:3.0.0.RELEASE")
@Grab("org.grails:grails-spring:2.3.6")
@Grab("com.h2database:h2:1.3.164")

import grails.orm.bootstrap.*
import grails.persistence.*
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.h2.Driver

init = new HibernateDatastoreSpringInitializer(Person)
def dataSource = new DriverManagerDataSource(Driver.name, "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE", 'sa', '')
init.configureForDataSource(dataSource)

@Entity
class Person {
    String firstName
    String lastName
    Date birthDate

    static Closure constraints = {
      Map nameConstrants = [blank:false]

      ['firstName', 'lastName'].each {
        "${it}"(nameConstrants)
      }

      delegate.birthDate([nullable:false])
    }

}

Person person = new Person()
assert person.validate() == false
assert person.errors.allErrors.size() == 3

person.firstName = 'Craig'
person.lastName = 'Burke'
assert person.validate() == false
assert person.errors.allErrors.size() == 1

person.birthDate = new Date()
assert person.validate()



