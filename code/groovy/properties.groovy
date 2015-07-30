Number.metaClass.getDollars = { delegate as BigDecimal }

Number.metaClass.getProperty = { String name ->
  def rates = [euros: 1.1f, pesos: 0.063f]
  delegate * (rates[name] as BigDecimal)
}

def total = 20.dollars + 40.euros + 200.pesos

println 500.pesos
println total