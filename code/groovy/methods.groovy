class Person {
	
  def methodMissing(String name, args) {	  
    if (name.startsWith('say')) {
	  String message = (name - 'say').trim()  	
      println message
    }
  }
  
}

Person you = new Person()
you.sayHello()
you."say Craig is Awesome"()
you."say unfollow @danveloper"()