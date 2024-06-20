package uk.gov.justice.digital.hmpps.personrecord.test.messages

class LibraMessage(
  val pncNumber: String? = "2003/0011985X",
  val firstName: String? = "Arthur",
  val lastName: String = "MORGAN",
  val dateOfBirth: String = "01/01/1975",
  val cro: String = "85227/65L",
  val postcode: String = "NT4 6YH",
)

fun libraHearing(libraMessage: LibraMessage = LibraMessage()) = """
{
   "caseId":1217464,
   "caseNo":"1600032981",
   "name":{
      "title":"Mr",
      ${libraMessage.firstName?.let { """ "forename1": "${libraMessage.firstName}", """.trimIndent() } ?: ""}
      "surname":"${libraMessage.lastName}"
   },
   "defendantName":"Mr ${libraMessage.firstName} ${libraMessage.lastName}",
   "defendantType":"P",
   "defendantSex":"N",
   "defendantDob":"${libraMessage.dateOfBirth}",
   "defendantAge":"20",
   "defendantAddress":{
      "line1": "39 The Street",
      "line2": "Newtown",
      "pcode": "${libraMessage.postcode}"
   },
   "cro":"${libraMessage.cro}",
   ${libraMessage.pncNumber?.let { """ "pnc": "${libraMessage.pncNumber}", """.trimIndent() } ?: ""}
   "listNo":"1st",
   "nationality1":"Angolan",
   "nationality2":"Austrian",
   "offences":[
      {
         "seq":1,
         "summary":"On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.",
         "title":"Theft from a shop",
         "act":"Contrary to section 1(1) and 7 of the Theft Act 1968."
      }
   ],
   "sessionStartTime":"2020-02-20T09:01:00",
   "courtCode":"B10JQ",
   "courtRoom":"00",
   "seq":1
}
""".trimIndent()
