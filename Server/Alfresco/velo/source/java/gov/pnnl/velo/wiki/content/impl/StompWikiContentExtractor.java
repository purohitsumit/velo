/*******************************************************************************
 * .
 *                           Velo 1.0
 *  ----------------------------------------------------------
 * 
 *            Pacific Northwest National Laboratory
 *                      Richland, WA 99352
 * 
 *                      Copyright (c) 2013
 *            Pacific Northwest National Laboratory
 *                 Battelle Memorial Institute
 * 
 *   Velo is an open-source collaborative content management
 *                and job execution environment
 *              distributed under the terms of the
 *           Educational Community License (ECL) 2.0
 *   A copy of the license is included with this distribution
 *                   in the LICENSE.TXT file
 * 
 *                        ACKNOWLEDGMENT
 *                        --------------
 * 
 * This software and its documentation were developed at Pacific 
 * Northwest National Laboratory, a multiprogram national
 * laboratory, operated for the U.S. Department of Energy by 
 * Battelle under Contract Number DE-AC05-76RL01830.
 ******************************************************************************/
package gov.pnnl.velo.wiki.content.impl;


import gov.pnnl.velo.util.MimetypeConstants;
import gov.pnnl.velo.util.WikiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 */
public class StompWikiContentExtractor extends AbstractWikiContentExtractor {


  /* (non-Javadoc)
   * @see gov.pnnl.velo.wiki.content.WikiContentExtractor#getSupportedMimetypes()
   */
  @Override
  public List<String> getSupportedMimetypes() {
    List<String> mimetypes = new ArrayList<String>();
    mimetypes.add(MimetypeConstants.MIMETYPE_STOMP_INPUT);
    return mimetypes;
  }

  /**
   * Method extractWikiContent.
   * @param alfrescoNode NodeRef
   * @return File
   * @throws Exception
   * @see gov.pnnl.velo.wiki.content.WikiContentExtractor#extractWikiContent(NodeRef)
   */
  @Override
  public File extractWikiContent(NodeRef alfrescoNode) throws Exception{
    String summary = "";

    String contentStr = getFileContentAsString(alfrescoNode);

    String startCard = "~Simulation Title Card";
    String endCard = "~Solution Control Card";
    int startPos = contentStr.indexOf(startCard) + startCard.length();
    int endPos = contentStr.indexOf(endCard);
    if(endPos != -1) {
      summary = contentStr.substring(startPos, endPos);
    }
    
    // Add default metadata template around summary
    StringBuilder metadata = new StringBuilder("");
    metadata.append("__NOTOC__\n\n"); 
    metadata.append("= Metadata  =\n\n");
    metadata.append("== Metadata for {{PAGENAME}}  ==\n\n");
    metadata.append(summary + "\n\n");
    metadata.append("= Provenance  =\n\n");
    metadata.append("There are no saved relationships to this file\n\n");
    metadata.append("=Annotation=\n\n");
    metadata.append("== User Annotation ==\n\n");
    metadata.append("<headertabs />\n\n");

    return WikiUtils.createTempFile(metadata.toString());

  }


}
