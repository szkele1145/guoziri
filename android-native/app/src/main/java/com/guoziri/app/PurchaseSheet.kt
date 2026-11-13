/** 简化版自动联动：填总价+斤 → 算单价；总价+单价 → 算斤 */
fun autoCalc(priceS: String, totalS: String, jinS: String, gramsS: String, src: String,
             setPrice: (String, String) -> Unit, setJin: (String, String) -> Unit) {
    val price = priceS.toDoubleOrNull() ?: return
    val total = totalS.toDoubleOrNull() ?: return
    val jin = jinS.toDoubleOrNull()
    when (src) {
        "total" -> if (jin != null && jin > 0) setPrice(priceS, (Math.round(total / jin * 100) / 100.0).toString())
        "price" -> if (total > 0 && jin == null) setJin(jinS, (Math.round(total / price * 100) / 100.0).toString())
        "jin" -> if (total > 0) setPrice(priceS, (Math.round(total / jin * 100) / 100.0).toString())
        "grams" -> if (total > 0) setJin(jinS, (Math.round(total / price * 100) / 100.0).toString())
    }
}

fun autoCalc2(jinS: String, gramsS: String, setGrams: (String) -> Unit) {
    val jin = jinS.toDoubleOrNull() ?: return
    if (gramsS.isBlank() || gramsS.toDoubleOrNull() == null) setGrams((jin * 500).toInt().toString())
}