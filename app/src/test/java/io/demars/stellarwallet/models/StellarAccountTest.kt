package io.demars.stellarwallet.models

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import org.apache.commons.lang.builder.HashCodeBuilder
import org.junit.Test
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.AccountResponse

class StellarAccountTest {
    @Test
    fun testHashcodeBuilder(){
        val val1 = HashCodeBuilder(17, 37)
                .append("this is a string")
                .append(Integer(12334))
                .append(13423L)
                .append("this is a string2")
                .toHashCode()

        val val2 = HashCodeBuilder(17, 37)
                .append("this is a string")
                .append(Integer(12334))
                .append(13423L)
                .append("this is a string2")
                .toHashCode()

        assertEquals(val1, val2)
    }

    @Test
    fun testHashCodeEqual(){
        //This are redundant tests with the implementation
        val keyPair = KeyPair.fromAccountId("GA5J2E65ERCMYNND7JTBZ57AZFDBYCQWSLWNKTSU4UIYD37MLZUU2T5C")
        val stellarAccount1 = StellarAccount(AccountResponse(keyPair, 1234))

        val keyPair2 = KeyPair.fromAccountId("GA5J2E65ERCMYNND7JTBZ57AZFDBYCQWSLWNKTSU4UIYD37MLZUU2T5C")
        val stellarAccount2 = StellarAccount(AccountResponse(keyPair2, 1234))

        assertEquals(stellarAccount1.basicHashCode(), stellarAccount2.basicHashCode())
    }

    @Test
    fun testHashCodeDifferent(){
        //This are redundant tests with the implementation
        val keyPair = KeyPair.fromAccountId("GDXVG5T344TBLPCYYTUTMJTWNU2DN6XV2IH3CQYNTNQ2JVG7IOSPTCP5")
        val accountResponse = StellarAccount(AccountResponse(keyPair, 1234))

        val keyPair2 = KeyPair.fromAccountId("GA7UNACGPOAITFO2APERDQ3ASUY2OFODYIBIFL42RZVMPBUKWEFW7MPJ")
        val accountResponse2 = StellarAccount(AccountResponse(keyPair2, 1235))

        assertNotSame(accountResponse.basicHashCode(), accountResponse2.basicHashCode())
    }
}