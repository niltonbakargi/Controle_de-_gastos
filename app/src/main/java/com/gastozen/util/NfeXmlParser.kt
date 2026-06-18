package com.gastozen.util

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ProdutoNfe(
    val nome: String,
    val ncm: String,
    val valor: Double
)

data class NotaFiscal(
    val produtos: List<ProdutoNfe>,
    val valorTotal: Double,
    val dataEmissao: String,
    val cnpjEmitente: String
)

object NfeXmlParser {

    /** Substitui entidades HTML (ex: &Ocirc;) por seus caracteres Unicode antes de parsear o XML. */
    private fun sanitizeHtmlEntities(xml: String): String {
        val entities = mapOf(
            "&Aacute;" to "Á", "&aacute;" to "á", "&Agrave;" to "À", "&agrave;" to "à",
            "&Acirc;" to "Â",  "&acirc;" to "â",  "&Atilde;" to "Ã", "&atilde;" to "ã",
            "&Auml;" to "Ä",   "&auml;" to "ä",   "&Aring;" to "Å",  "&aring;" to "å",
            "&Eacute;" to "É", "&eacute;" to "é", "&Egrave;" to "È", "&egrave;" to "è",
            "&Ecirc;" to "Ê",  "&ecirc;" to "ê",  "&Euml;" to "Ë",  "&euml;" to "ë",
            "&Iacute;" to "Í", "&iacute;" to "í", "&Igrave;" to "Ì", "&igrave;" to "ì",
            "&Icirc;" to "Î",  "&icirc;" to "î",  "&Iuml;" to "Ï",  "&iuml;" to "ï",
            "&Oacute;" to "Ó", "&oacute;" to "ó", "&Ograve;" to "Ò", "&ograve;" to "ò",
            "&Ocirc;" to "Ô",  "&ocirc;" to "ô",  "&Otilde;" to "Õ", "&otilde;" to "õ",
            "&Ouml;" to "Ö",   "&ouml;" to "ö",   "&Oslash;" to "Ø", "&oslash;" to "ø",
            "&Uacute;" to "Ú", "&uacute;" to "ú", "&Ugrave;" to "Ù", "&ugrave;" to "ù",
            "&Ucirc;" to "Û",  "&ucirc;" to "û",  "&Uuml;" to "Ü",  "&uuml;" to "ü",
            "&Ccedil;" to "Ç", "&ccedil;" to "ç", "&Ntilde;" to "Ñ", "&ntilde;" to "ñ",
            "&nbsp;" to " ",   "&ndash;" to "–",  "&mdash;" to "—",
            "&copy;" to "©",   "&reg;" to "®",    "&trade;" to "™",
            "&laquo;" to "«",  "&raquo;" to "»",
        )
        var result = xml
        entities.forEach { (entity, char) -> result = result.replace(entity, char) }
        return result
    }

    fun parse(xml: String): NotaFiscal {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(sanitizeHtmlEntities(xml).reader())

        val produtos = mutableListOf<ProdutoNfe>()
        var valorTotal = 0.0
        var dataEmissao = ""
        var cnpjEmitente = ""

        var nomeProd = ""
        var ncmProd = ""
        var valorProd = 0.0
        var inProd = false
        var inTotal = false
        var inEmit = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "prod" -> { inProd = true; nomeProd = ""; ncmProd = ""; valorProd = 0.0 }
                        "total" -> inTotal = true
                        "emit" -> inEmit = true
                        "xProd" -> if (inProd) nomeProd = parser.nextText()
                        "NCM"  -> if (inProd) ncmProd = parser.nextText()
                        "vProd" -> if (inProd) valorProd = parser.nextText().toDoubleOrNull() ?: 0.0
                        "vNF"  -> if (inTotal) valorTotal = parser.nextText().toDoubleOrNull() ?: 0.0
                        "dhEmi" -> if (!inProd && !inEmit) dataEmissao = parser.nextText()
                        "CNPJ" -> if (inEmit) cnpjEmitente = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "prod"  -> {
                            if (inProd && nomeProd.isNotBlank()) {
                                produtos.add(ProdutoNfe(nomeProd, ncmProd, valorProd))
                            }
                            inProd = false
                        }
                        "total" -> inTotal = false
                        "emit"  -> inEmit = false
                    }
                }
            }
            eventType = parser.next()
        }
        return NotaFiscal(produtos, valorTotal, dataEmissao, cnpjEmitente)
    }
}
