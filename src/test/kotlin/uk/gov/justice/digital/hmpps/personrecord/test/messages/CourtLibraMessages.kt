package uk.gov.justice.digital.hmpps.personrecord.test.messages

fun libraHearing(pncNumber: String? = "2003/0011985X", firstName: String? = "Arthur", lastName: String = "MORGAN", dateOfBirth: String = "01/01/1975", cro: String = "85227/65L", postcode: String = "NT4 6YH") = """
{
   "caseId":1217464,
   "caseNo":"1600032981",
   "name":{
      "title":"Mr",
      ${firstName?.let { """ "forename1": "$firstName", """.trimIndent() } ?: ""}
      "surname":"$lastName"
   },
   "defendantName":"Mr $firstName $lastName",
   "defendantType":"P",
   "defendantSex":"N",
   "defendantDob":"$dateOfBirth",
   "defendantAge":"20",
   "defendantAddress":{
      "line1": "39 The Street",
      "line2": "Newtown",
      "pcode": "$postcode"
   },
   "cro":"$cro",
   ${pncNumber?.let { """ "pnc": "$pncNumber", """.trimIndent() } ?: ""}
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
