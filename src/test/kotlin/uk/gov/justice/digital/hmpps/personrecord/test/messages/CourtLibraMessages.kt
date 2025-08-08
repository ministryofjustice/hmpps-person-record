package uk.gov.justice.digital.hmpps.personrecord.test.messages

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

fun libraHearing(
  pncNumber: String? = randomPnc(),
  title: String? = null,
  firstName: String? = null,
  foreName2: String? = null,
  foreName3: String? = null,
  lastName: String = randomName(),
  dateOfBirth: String = "",
  cro: String = randomCro(),
  postcode: String = randomPostcode(),
  line1: String? = randomName(),
  line2: String? = randomName(),
  line3: String? = randomName(),
  line4: String? = randomName(),
  line5: String? = randomName(),
  cId: String = randomCId(),
  defendantType: DefendantType = PERSON,
  defendantSex: String? = null,
  nationality1: String? = null,
  nationality2: String? = null,
) = """
{
   "cId": "$cId",
   "caseNo":"1600032981",
   "name":{
      ${title?.let { """ "title": "$title", """.trimIndent() } ?: ""}
      ${firstName?.let { """ "forename1": "$firstName", """.trimIndent() } ?: ""}
      ${foreName2?.let { """ "forename2": "$foreName2", """.trimIndent() } ?: ""}
      ${foreName3?.let { """ "forename3": "$foreName3", """.trimIndent() } ?: ""}
      "surname":"$lastName"
   },
   "defendantName":"Mr $firstName $lastName",
   "defendantType": "${defendantType.value}",
   "defendantSex":"$defendantSex",
   "defendantDob":"$dateOfBirth",
   "defendantAge":"20",
   "defendantAddress":{
      ${line1?.let { """ "line1": "$line1", """.trimIndent() } ?: ""}
      ${line2?.let { """ "line2": "$line2", """.trimIndent() } ?: ""}
      ${line3?.let { """ "line3": "$line3", """.trimIndent() } ?: ""}
      ${line4?.let { """ "line4": "$line4", """.trimIndent() } ?: ""}
      ${line5?.let { """ "line5": "$line5" """.trimIndent() } ?: ""}
      ${postcode.let { """ ,"pcode": "$postcode" """.trimIndent() }}
   },
   "cro":"$cro",
   ${pncNumber?.let { """ "pnc": "$pncNumber", """.trimIndent() } ?: ""}
   "listNo":"1st",
   ${nationality1?.let { """ "nationality1": "$nationality1", """.trimIndent() } ?: ""}
   ${nationality2?.let { """ "nationality1": "$nationality2", """.trimIndent() } ?: ""}
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
