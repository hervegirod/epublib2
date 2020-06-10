package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import nl.siegmann.epublib.util.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Guide;
import nl.siegmann.epublib.domain.GuideReference;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;

import nl.siegmann.epublib.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the opf package document as defined by namespace http://www.idpf.org/2007/opf
 *
 * @author paul
 * @version 1.1
 */
public class PackageDocumentReader extends PackageDocumentBase {
   private static final String[] POSSIBLE_NCX_ITEM_IDS = new String[] { "toc", "ncx", "ncxtoc" };

   public static void read(Resource packageResource, EpubReader epubReader, Book book, Resources resources) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
      Document packageDocument = ResourceUtil.getAsDocument(packageResource);
      String packageHref = packageResource.getHref();
      resources = fixHrefs(packageHref, resources);
      readGuide(packageDocument, epubReader, book, resources);

      // Books sometimes use non-identifier ids. We map these here to legal ones
      Map<String, String> idMapping = new HashMap<String, String>();

      resources = readManifest(packageDocument, packageHref, epubReader, resources, idMapping);
      book.setResources(resources);
      readCover(packageDocument, book);
      book.setMetadata(PackageDocumentMetadataReader.readMetadata(packageDocument));
      book.setSpine(readSpine(packageDocument, book.getResources(), idMapping));

      // if we did not find a cover page then we make the first page of the book the cover page
      if (book.getCoverPage() == null && book.getSpine().size() > 0) {
         book.setCoverPage(book.getSpine().getResource(0));
      }
   }

//	private static Resource readCoverImage(Element metadataElement, Resources resources) {
//		String coverResourceId = DOMUtil.getFindAttributeValue(metadataElement.getOwnerDocument(), NAMESPACE_OPF, OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover, OPFAttributes.content);
//		if (StringUtil.isBlank(coverResourceId)) {
//			return null;
//		}
//		Resource coverResource = resources.getByIdOrHref(coverResourceId);
//		return coverResource;
//	}
   /**
    * Reads the manifest containing the resource ids, hrefs and mediatypes.
    *
    * @param packageDocument
    * @param packageHref
    * @param epubReader
    * @param book
    * @param resourcesByHref
    * @return a Map with resources, with their id's as key.
    */
   private static Resources readManifest(Document packageDocument, String packageHref,
      EpubReader epubReader, Resources resources, Map<String, String> idMapping) {
      Element manifestElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.manifest);
      Resources result = new Resources();
      if (manifestElement == null) {
         ErrorManager.error("Package document does not contain element " + OPFTags.manifest);
         return result;
      }
      NodeList itemElements = manifestElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.item);
      for (int i = 0; i < itemElements.getLength(); i++) {
         Element itemElement = (Element) itemElements.item(i);
         String id = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.id);
         String href = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.href);
         try {
            href = URLDecoder.decode(href, Constants.CHARACTER_ENCODING);
         } catch (UnsupportedEncodingException e) {
            ErrorManager.error(e);
         }
         String mediaTypeName = DOMUtil.getAttribute(itemElement, NAMESPACE_OPF, OPFAttributes.media_type);
         Resource resource = resources.remove(href);
         if (resource == null) {
            ErrorManager.error("resource with href '" + href + "' not found");
            continue;
         }
         resource.setId(id);
         MediaType mediaType = MediatypeService.getMediaTypeByName(mediaTypeName);
         if (mediaType != null) {
            resource.setMediaType(mediaType);
         }
         result.add(resource);
         idMapping.put(id, resource.getId());
      }
      return result;
   }

   /**
    * Reads the book's guide.
    * Here some more attempts are made at finding the cover page.
    *
    * @param packageDocument
    * @param epubReader
    * @param book
    * @param resources
    */
   private static void readGuide(Document packageDocument,
      EpubReader epubReader, Book book, Resources resources) {
      Element guideElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.guide);
      if (guideElement == null) {
         return;
      }
      Guide guide = book.getGuide();
      NodeList guideReferences = guideElement.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.reference);
      for (int i = 0; i < guideReferences.getLength(); i++) {
         Element referenceElement = (Element) guideReferences.item(i);
         String resourceHref = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.href);
         if (StringUtil.isEmpty(resourceHref)) {
            continue;
         }
         Resource resource = resources.getByHref(StringUtil.substringBefore(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
         if (resource == null) {
            ErrorManager.error("Guide is referencing resource with href " + resourceHref + " which could not be found");
            continue;
         }
         String type = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.type);
         if (StringUtil.isEmpty(type)) {
            ErrorManager.error("Guide is referencing resource with href " + resourceHref + " which is missing the 'type' attribute");
            continue;
         }
         String title = DOMUtil.getAttribute(referenceElement, NAMESPACE_OPF, OPFAttributes.title);
         if (GuideReference.COVER.equalsIgnoreCase(type)) {
            continue; // cover is handled elsewhere
         }
         GuideReference reference = new GuideReference(resource, type, title, StringUtil.substringAfter(resourceHref, Constants.FRAGMENT_SEPARATOR_CHAR));
         guide.addReference(reference);
      }
   }

   /**
    * Strips off the package prefixes up to the href of the packageHref.
    *
    * Example:
    * If the packageHref is "OEBPS/content.opf" then a resource href like "OEBPS/foo/bar.html" will be turned into "foo/bar.html"
    *
    * @param packageHref
    * @param resourcesByHref
    * @return The stripped package href
    */
   static Resources fixHrefs(String packageHref,
      Resources resourcesByHref) {
      int lastSlashPos = packageHref.lastIndexOf('/');
      if (lastSlashPos < 0) {
         return resourcesByHref;
      }
      Resources result = new Resources();
      for (Resource resource : resourcesByHref.getAll()) {
         if (!StringUtil.isEmpty(resource.getHref())
            && resource.getHref().length() > lastSlashPos) {
            resource.setHref(resource.getHref().substring(lastSlashPos + 1));
         }
         result.add(resource);
      }
      return result;
   }

   /**
    * Reads the document's spine, containing all sections in reading order.
    *
    * @param packageDocument
    * @param epubReader
    * @param book
    * @param resourcesById
    * @return the document's spine, containing all sections in reading order.
    */
   private static Spine readSpine(Document packageDocument, Resources resources, Map<String, String> idMapping) {

      Element spineElement = DOMUtil.getFirstElementByTagNameNS(packageDocument.getDocumentElement(), NAMESPACE_OPF, OPFTags.spine);
      if (spineElement == null) {
         ErrorManager.error("Element " + OPFTags.spine + " not found in package document, generating one automatically");
         return generateSpineFromResources(resources);
      }
      Spine result = new Spine();
      String tocResourceId = DOMUtil.getAttribute(spineElement, NAMESPACE_OPF, OPFAttributes.toc);
      result.setTocResource(findTableOfContentsResource(tocResourceId, resources));
      NodeList spineNodes = packageDocument.getElementsByTagNameNS(NAMESPACE_OPF, OPFTags.itemref);
      List<SpineReference> spineReferences = new ArrayList<SpineReference>(spineNodes.getLength());
      for (int i = 0; i < spineNodes.getLength(); i++) {
         Element spineItem = (Element) spineNodes.item(i);
         String itemref = DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.idref);
         if (StringUtil.isEmpty(itemref)) {
            ErrorManager.error("itemref with missing or empty idref"); // XXX
            continue;
         }
         String id = idMapping.get(itemref);
         if (id == null) {
            id = itemref;
         }
         Resource resource = resources.getByIdOrHref(id);
         if (resource == null) {
            ErrorManager.error("resource with id \'" + id + "\' not found");
            continue;
         }

         SpineReference spineReference = new SpineReference(resource);
         if (OPFValues.no.equalsIgnoreCase(DOMUtil.getAttribute(spineItem, NAMESPACE_OPF, OPFAttributes.linear))) {
            spineReference.setLinear(false);
         }
         spineReferences.add(spineReference);
      }
      result.setSpineReferences(spineReferences);
      return result;
   }

   /**
    * Creates a spine out of all resources in the resources.
    * The generated spine consists of all XHTML pages in order of their href.
    *
    * @param resources
    * @return a spine created out of all resources in the resources.
    */
   private static Spine generateSpineFromResources(Resources resources) {
      Spine result = new Spine();
      List<String> resourceHrefs = new ArrayList<>();
      resourceHrefs.addAll(resources.getAllHrefs());
      Collections.sort(resourceHrefs, String.CASE_INSENSITIVE_ORDER);
      for (String resourceHref : resourceHrefs) {
         Resource resource = resources.getByHref(resourceHref);
         if (resource.getMediaType() == MediatypeService.NCX) {
            result.setTocResource(resource);
         } else if (resource.getMediaType() == MediatypeService.XHTML) {
            result.addSpineReference(new SpineReference(resource));
         }
      }
      return result;
   }

   /**
    * The spine tag should contain a 'toc' attribute with as value the resource id of the table of contents resource.
    *
    * Here we try several ways of finding this table of contents resource.
    * We try the given attribute value, some often-used ones and finally look through all resources for the first resource with the table of contents mimetype.
    *
    * @param spineElement
    * @param resourcesById
    * @return the Resource containing the table of contents
    */
   static Resource findTableOfContentsResource(String tocResourceId, Resources resources) {
      Resource tocResource = null;
      if (!StringUtil.isEmpty(tocResourceId)) {
         tocResource = resources.getByIdOrHref(tocResourceId);
      }

      if (tocResource != null) {
         return tocResource;
      }

      // get the first resource with the NCX mediatype
      tocResource = resources.findFirstResourceByMediaType(MediatypeService.NCX);

      if (tocResource == null) {
         for (int i = 0; i < POSSIBLE_NCX_ITEM_IDS.length; i++) {
            tocResource = resources.getByIdOrHref(POSSIBLE_NCX_ITEM_IDS[i]);
            if (tocResource != null) {
               break;
            }
            tocResource = resources.getByIdOrHref(POSSIBLE_NCX_ITEM_IDS[i].toUpperCase());
            if (tocResource != null) {
               break;
            }
         }
      }

      if (tocResource == null) {
         ErrorManager.error("Could not find table of contents resource. Tried resource with id '" + tocResourceId + "', " + Constants.DEFAULT_TOC_ID + ", " + Constants.DEFAULT_TOC_ID.toUpperCase() + " and any NCX resource.");
      }
      return tocResource;
   }

   /**
    * Find all resources that have something to do with the coverpage and the cover image.
    * Search the meta tags and the guide references
    *
    * @param packageDocument
    * @return all resources that have something to do with the coverpage and the cover image.
    */
   // package
   static Set<String> findCoverHrefs(Document packageDocument) {
      Set<String> result = new HashSet<>();

      // try and find a meta tag with name = 'cover' and a non-blank id
      String coverResourceId = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
         OPFTags.meta, OPFAttributes.name, OPFValues.meta_cover,
         OPFAttributes.content);

      if (!StringUtil.isEmpty(coverResourceId)) {
         String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
            OPFTags.item, OPFAttributes.id, coverResourceId,
            OPFAttributes.href);
         if (!StringUtil.isEmpty(coverHref)) {
            result.add(coverHref);
         } else {
            result.add(coverResourceId); // maybe there was a cover href put in the cover id attribute
         }
      }
      // try and find a reference tag with type is 'cover' and reference is not blank
      String coverHref = DOMUtil.getFindAttributeValue(packageDocument, NAMESPACE_OPF,
         OPFTags.reference, OPFAttributes.type, OPFValues.reference_cover,
         OPFAttributes.href);
      if (!StringUtil.isEmpty(coverHref)) {
         result.add(coverHref);
      }
      return result;
   }

   /**
    * Finds the cover resource in the packageDocument and adds it to the book if found.
    * Keeps the cover resource in the resources map
    *
    * @param packageDocument
    * @param book
    * @param resources
    */
   private static void readCover(Document packageDocument, Book book) {

      Collection<String> coverHrefs = findCoverHrefs(packageDocument);
      for (String coverHref : coverHrefs) {
         Resource resource = book.getResources().getByHref(coverHref);
         if (resource == null) {
            ErrorManager.error("Cover resource " + coverHref + " not found");
            continue;
         }
         if (resource.getMediaType() == MediatypeService.XHTML) {
            book.setCoverPage(resource);
         } else if (MediatypeService.isBitmapImage(resource.getMediaType())) {
            book.setCoverImage(resource);
         }
      }
   }

}
