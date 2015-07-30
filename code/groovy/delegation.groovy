Closure myClosure = {
  name = 'Dane Cook'
  printName()
}

class NamePrinter {
  String name

  void printName() {
    println "My Name is ${name}!!!"
  }
}

myClosure.delegate = new NamePrinter()
myClosure.resolveStrategy = Closure.DELEGATE_FIRST
myClosure()