/**
 * 
 */
package br.uerj.lampada.openehr.susbuilder.printer.impl;

import java.util.List;

import org.openehr.rm.composition.Composition;

import br.uerj.lampada.openehr.susbuilder.printer.EHRObjectPrinter;
import br.uerj.lampada.openehr.susbuilder.printer.EHRPrintCore;
import br.uerj.lampada.openehr.susbuilder.utils.Constants;

/**
 * @author teodoro
 * 
 */
public class CompositionPrinter extends EHRPrintCore implements
		EHRObjectPrinter {

	public CompositionPrinter(String outputFolder, String format) {
		super(outputFolder, format);
	}

	/**
	 * Write Compositions
	 * 
	 * @param uuid
	 * @param compositions
	 * @param dirName
	 * @throws Exception
	 */
	@Override
	public void writeOpenEHRObject(String uuid, String outputFolder,
			List<Composition> compositions) throws Exception {
		// Write Composition
		for (int i = 0; i < compositions.size(); i++) {
			Composition composition = compositions.get(i);

			String docId = Constants.COMPOSITION_UUID_PREFIX + (i + 1) + "."
					+ uuid;
			String filename = outputFolder + "/" + docId + "." + getFormat();

			writeInFormat(composition, filename);
		}
	}

}
