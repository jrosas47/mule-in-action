
package com.muleinaction.eventcontext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.SimpleDataType;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author David Dossot (david@dossot.net)
 */
public class ProspectingMessage
{

    public final static class BigIntegerToBytesTransformer extends AbstractDiscoverableTransformer
    {

        public int usageCount = 0;

        public BigIntegerToBytesTransformer()
        {
            super();
            registerSourceType(new SimpleDataType<BigInteger>(BigInteger.class));
            setReturnDataType(new SimpleDataType<byte[]>(byte[].class));
        }

        @Override
        protected Object doTransform(final Object src, final String encoding) throws TransformerException
        {

            usageCount++;

            return ((BigInteger) src).toByteArray();
        }

    }

    private static MuleClient client;

    private static BigIntegerToBytesTransformer BITBT = new BigIntegerToBytesTransformer();

    private static MuleContext getMuleContext()
    {
        return client.getMuleContext();
    }

    @BeforeClass
    public static void bootstrapMule() throws Exception
    {
        client = new MuleClient(true);

        getMuleContext().getRegistry().registerTransformer(BITBT);
    }

    @Before
    public void resetBigIntegerToBytesTransformerUsageCount()
    {
        BITBT.usageCount = 0;
    }

    @AfterClass
    public static void disposeMule() throws Exception
    {
        client.dispose();
    }

    @Test
    public void defaultTransformersExist()
    {
        // we should have more than our own transformer in the registry
        assertTrue(getMuleContext().getRegistry().getTransformers().size() > 1);
    }

    @Test
    public void stringPayloadAsBytesAndString() throws Exception
    {
        final String payload = "foo";

        final MuleMessage message = new DefaultMuleMessage(payload, (Map<String, Object>) null,
            getMuleContext());

        assertTrue(Arrays.equals(payload.getBytes(message.getEncoding()), message.getPayloadAsBytes()));
        assertEquals(payload, message.getPayloadAsString());

        // the bytes and string rendering do no alter the payload
        assertEquals(payload, message.getPayload());
        assertEquals(payload, message.getOriginalPayload());
    }

    @Test
    public void serializablePayloadAsBytesAndString() throws Exception
    {
        final BigInteger payload = BigInteger.valueOf(123L);

        final MuleMessage message = new DefaultMuleMessage(payload, (Map<String, Object>) null,
            getMuleContext());

        assertEquals(0, BITBT.usageCount);
        assertTrue(Arrays.equals(payload.toByteArray(), message.getPayloadAsBytes()));
        assertEquals(new String(payload.toByteArray(), message.getEncoding()), message.getPayloadAsString());
        assertEquals(1, BITBT.usageCount);
        assertTrue(Arrays.equals(payload.toByteArray(), message.getPayloadAsBytes()));
        assertEquals(new String(payload.toByteArray(), message.getEncoding()), message.getPayloadAsString());

        // the usage count has not changed because we have hit the byte cache
        // inside the message
        assertEquals(1, BITBT.usageCount);

        // the bytes and string rendering do no alter the payload
        assertEquals(payload, message.getPayload());
        assertEquals(payload, message.getOriginalPayload());
    }

    @Test
    public void serializablePayloadTransformation() throws Exception
    {
        final BigInteger payload = BigInteger.valueOf(987L);

        final MuleMessage message = new DefaultMuleMessage(payload, (Map<String, Object>) null,
            getMuleContext());

        assertEquals(0, BITBT.usageCount);

        // usually the transformers are defined on the endpoints
        message.applyTransformers(null, Collections.singletonList(BITBT));

        assertEquals(1, BITBT.usageCount);
        assertTrue(message.getPayload() instanceof byte[]);
        assertTrue(Arrays.equals(payload.toByteArray(), (byte[]) message.getPayload()));
        assertEquals(payload, message.getOriginalPayload());
    }
}
