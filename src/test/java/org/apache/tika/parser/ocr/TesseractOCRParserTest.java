/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.ocr;

import static org.apache.tika.parser.ocr.TesseractOCRParser.getTesseractProg;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import java.util.List;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.junit.Test;
import org.xml.sax.helpers.DefaultHandler;
public class TesseractOCRParserTest extends TikaTest {

    public static boolean canRunCheck() {
        TesseractOCRConfig config = new TesseractOCRConfig();
        TesseractOCRParserTest tesseractOCRTest = new TesseractOCRParserTest();
        return tesseractOCRTest.canRunCheck(config);
    }

    private boolean canRunCheck(TesseractOCRConfig config) {
        String[] checkCmd = {config.getTesseractPath() + getTesseractProg()};
        // If Tesseract is not on the path, do not run the test.
        return ExternalParser.check(checkCmd);
    }


  // @Test
    public void testMyPDFOCR() throws Exception {
        String resource = "/test-documents/SidikouInvitationFamille.pdf";
        String[] nonOCRContains = {
                "Harouna",
                "technologies"
        };
        testMyBasicOCR(resource, nonOCRContains, 2);
    }


    private void testMyBasicOCR(String resource, String[] nonOCRContains, int numMetadatas) throws Exception {
    	InputStream  inputStream = TesseractOCRParserTest.class.getResourceAsStream(resource);
    	Metadata metadata = new Metadata();
    	String suffix = "full";
    	//suffix = "default";
    	InputStream tessCfg =  TesseractOCRParserTest.class.getResourceAsStream("/test-properties/TesseractOCRConfig-"+suffix+".properties");
    	TesseractOCRConfig config = new TesseractOCRConfig(tessCfg);
        Parser parser = new RecursiveParserWrapper(new AutoDetectParser(),
                new BasicContentHandlerFactory(
                        BasicContentHandlerFactory.HANDLER_TYPE.TEXT, 1000));

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);

        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        parseContext.set(Parser.class, parser);
        parseContext.set(PDFParserConfig.class, pdfConfig);

        DefaultHandler handler = new DefaultHandler();
		if (canRunCheck()) {
	        parser.parse(inputStream, handler, metadata, parseContext);
	
	        List<Metadata> metadataList = ((RecursiveParserWrapper) parser).getMetadata();
	    
	        StringBuilder contents = new StringBuilder();
	        int count = 1;
	        for (Metadata m : metadataList) {
	        	 String str = m.get(RecursiveParserWrapper.TIKA_CONTENT);
	        	 if (str != null){
	 	            str = str.replaceAll("\n+", "\n");
		            System.out.println(count++ +"--------------------------------------------------------------------\n" + str);
					contents.append(str);	 
	        	 }
	        }
	        String extractedText = contents.toString();
	      //  extractedText = extractedText.replaceAll("\n+", "\n");
	       // System.out.println("--------------------------------------------------------------------\n" + extractedText);
				for (String needle : nonOCRContains) {
					assertContains(needle, extractedText);
				}
        } else {
        	fail("Tessaract not configured");
        }
    }
    /*
    Check that if Tesseract is not found, the TesseractOCRParser claims to not support
    any file types. So, the standard image parser is called instead.
     */
    @Test
    public void offersNoTypesIfNotFound() throws Exception {
        TesseractOCRParser parser = new TesseractOCRParser();
        DefaultParser defaultParser = new DefaultParser();
        MediaType png = MediaType.image("png");

        // With an invalid path, will offer no types
        TesseractOCRConfig invalidConfig = new TesseractOCRConfig();
        invalidConfig.setTesseractPath("/made/up/path");

        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, invalidConfig);

        // No types offered
        assertEquals(0, parser.getSupportedTypes(parseContext).size());

        // And DefaultParser won't use us
        assertEquals(ImageParser.class, defaultParser.getParsers(parseContext).get(png).getClass());
    }

    /*
    If Tesseract is found, test we retrieve the proper number of supporting Parsers.
     */
    @Test
    public void offersTypesIfFound() throws Exception {
        TesseractOCRParser parser = new TesseractOCRParser();
        DefaultParser defaultParser = new DefaultParser();

        ParseContext parseContext = new ParseContext();
        MediaType png = MediaType.image("png");

        // Assuming that Tesseract is on the path, we should find 5 Parsers that support PNG.
        assumeTrue(canRunCheck());

        assertEquals(5, parser.getSupportedTypes(parseContext).size());
        assertTrue(parser.getSupportedTypes(parseContext).contains(png));

        // DefaultParser will now select the TesseractOCRParser.
        assertEquals(TesseractOCRParser.class, defaultParser.getParsers(parseContext).get(png).getClass());
    }

    @Test
    public void testPDFOCR() throws Exception {
        String resource = "/test-documents/testOCR.pdf";
        String[] nonOCRContains = { "New", "2003"  };
        testBasicOCR(resource, nonOCRContains, 2);
    }

    @Test
    public void testDOCXOCR() throws Exception {
        String resource = "/test-documents/testOCR.docx";
        String[] nonOCRContains = {
                "This is some text.",
                "Here is an embedded image:"
        };
        testBasicOCR(resource, nonOCRContains, 3);
    }

    @Test
    public void testPPTXOCR() throws Exception {
        String resource = "/test-documents/testOCR.pptx";
        String[] nonOCRContains = {
                "This is some text"
        };
        testBasicOCR(resource, nonOCRContains, 3);
    }
    
    @Test
    public void testOCROutputsHOCR() throws Exception {
        assumeTrue(canRunCheck());

        String resource = "/test-documents/testOCR.pdf";

        String[] nonOCRContains = new String[0];
        String contents = runOCR(resource, nonOCRContains, 2,
                BasicContentHandlerFactory.HANDLER_TYPE.XML,
                TesseractOCRConfig.OUTPUT_TYPE.HOCR);

        assertContains("<span class=\"ocrx_word\" id=\"word_1_1\"", contents);
        assertContains("Happy</span>", contents);

    }

    private void testBasicOCR(String resource, String[] nonOCRContains, int numMetadatas) throws Exception{
    	String contents = runOCR(resource, nonOCRContains, numMetadatas,
                BasicContentHandlerFactory.HANDLER_TYPE.TEXT, TesseractOCRConfig.OUTPUT_TYPE.TXT);
        if (canRunCheck()) {
        	if(resource.substring(resource.lastIndexOf('.'), resource.length()).equals(".jpg")) {
        		assertTrue(contents.toString().contains("Apache"));
        	} else {
        		assertTrue(contents.toString().contains("Happy New Year 2003!"));
        	}
        }
    }
    
    private String runOCR(String resource, String[] nonOCRContains, int numMetadatas,
                          BasicContentHandlerFactory.HANDLER_TYPE handlerType,
                          TesseractOCRConfig.OUTPUT_TYPE outputType) throws Exception {
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setOutputType(outputType);
        
        Parser parser = new RecursiveParserWrapper(new AutoDetectParser(),
                new BasicContentHandlerFactory(
                        handlerType, -1));

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);

        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        parseContext.set(Parser.class, parser);
        parseContext.set(PDFParserConfig.class, pdfConfig);

        try (InputStream stream = TesseractOCRParserTest.class.getResourceAsStream(resource)) {
            parser.parse(stream, new DefaultHandler(), new Metadata(), parseContext);
        }
        List<Metadata> metadataList = ((RecursiveParserWrapper) parser).getMetadata();
        assertEquals(numMetadatas, metadataList.size());

        StringBuilder contents = new StringBuilder();
        for (Metadata m : metadataList) {
            contents.append(m.get(RecursiveParserWrapper.TIKA_CONTENT));
        }
 
        for (String needle : nonOCRContains) {
            assertContains(needle, contents.toString());
        }
        assertTrue(metadataList.get(0).names().length > 10);
        assertTrue(metadataList.get(1).names().length > 10);
        //test at least one value
        assertEquals("deflate", metadataList.get(1).get("Compression CompressionTypeName"));
        
        return contents.toString();
    }

    @Test
    public void testSingleImage() throws Exception {
        assumeTrue(canRunCheck());
        String xml = getXML("testOCR.jpg").xml;
        assertContains("OCR Testing", xml);
        //test metadata extraction
        assertContains("<meta name=\"Image Width\" content=\"136 pixels\" />", xml);

        //TIKA-2169
        assertContainsCount("<html", xml, 1);
        assertContainsCount("<title", xml, 1);
        assertContainsCount("</title", xml, 1);
        assertContainsCount("<body", xml, 1);
        assertContainsCount("</body", xml, 1);
        assertContainsCount("</html", xml, 1);
    }

    @Test
    public void testImageMagick() throws Exception {
    	InputStream stream = TesseractOCRConfig.class.getResourceAsStream(
                "/test-properties/TesseractOCR.properties");
    	TesseractOCRConfig config = new TesseractOCRConfig(stream);
    	String[] CheckCmd = {config.getImageMagickPath() + TesseractOCRParser.getImageMagickProg()};
    	assumeTrue(ExternalParser.check(CheckCmd));
    }
    
    @Test
    public void getNormalMetadataToo() throws Exception {
        //this should be successful whether or not TesseractOCR is installed/active
        //If tesseract is installed, the internal metadata extraction parser should
        //work; and if tesseract isn't installed, the regular parsers should take over.

        //gif
        Metadata m = getXML("testGIF.gif").metadata;
        assertTrue(m.names().length > 20);
        assertEquals("RGB", m.get("Chroma ColorSpaceType"));

        //jpg
        m = getXML("testOCR.jpg").metadata;
        assertEquals("136", m.get(Metadata.IMAGE_WIDTH));
        assertEquals("66", m.get(Metadata.IMAGE_LENGTH));
        assertEquals("8", m.get(Metadata.BITS_PER_SAMPLE));
        assertEquals(null, m.get(Metadata.SAMPLES_PER_PIXEL));
        String imageComment = m.get(Metadata.COMMENT);
		assertContains("This is a test Apache Tika imag", imageComment);

    }


}