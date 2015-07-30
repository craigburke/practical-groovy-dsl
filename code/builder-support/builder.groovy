class MyBuilder extends BuilderSupport {  
	
	def createNode(name) {
		println "METHOD: createNode(${name})"
		name
	}
	
	def createNode(name, value) { }
	def createNode(name, Map attributes) { }
	def createNode(name, Map attributes, value) { }
	
	void setParent(parent, child){
		println "METHOD: setParent(${parent}, ${child})"
	}
	void nodeCompleted(parent, node) {
		println "METHOD: nodeCompleted(${parent}, ${node})"
	}
}

def myBuilder = new MyBuilder()

myBuilder.foo {
	println "CLOSURE: foo run with delegate: ${delegate.getClass()}"
	
	bar {
		println "CLOSURE: bar run with delegate: ${delegate.getClass()}"
	}
}