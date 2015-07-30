@Grab(group='com.craigburke.document', module='pdf', version='0.4.6')
@Grab(group='com.craigburke.document', module='word', version='0.4.5')

import com.craigburke.document.builder.PdfDocumentBuilder
import com.craigburke.document.builder.WordDocumentBuilder

System.setProperty('java.awt.headless', 'true')
new File('documents').eachFile { it.delete() }

def builders = [
	new WordDocumentBuilder(new File('documents/example.docx')),
	new PdfDocumentBuilder(new File('documents/example.pdf'))
]

def document = { Closure closure ->
 	builders.each { builder ->
		Closure clonedClosure = closure.rehydrate(builder, this, this)
		
		builder.create {
			document clonedClosure
		}
	}
}

def binding = new Binding() 
def shell = new GroovyShell(binding)
binding.setVariable('document', document)

shell.evaluate(new File('command.groovy'))

println 'Done!'