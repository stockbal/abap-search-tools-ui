package com.devepos.adt.saat.internal.cdsanalysis;

import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.devepos.adt.saat.internal.elementinfo.IElementInfo;
import com.devepos.adt.saat.internal.elementinfo.IElementInfoCollection;
import com.devepos.adt.saat.internal.elementinfo.IElementInfoProvider;
import com.devepos.adt.saat.internal.messages.Messages;

public class CdsFieldTopDownElementInfoProvider implements IElementInfoProvider {

	private final String cdsViewName;
	private final String field;
	private final String destinationId;

	public CdsFieldTopDownElementInfoProvider(final String destinationId, final String cdsViewName, final String field) {
		this.cdsViewName = cdsViewName;
		this.field = field;
		this.destinationId = destinationId;
	}

	@Override
	public List<IElementInfo> getElements() {
		final IElementInfo cdsTopDownInfo = CdsAnalysisServiceFactory.createCdsAnalysisService()
			.loadTopDownFieldAnalysis(this.cdsViewName, this.field, this.destinationId);
		if (cdsTopDownInfo != null) {
			return ((IElementInfoCollection) cdsTopDownInfo).getChildren();
		}
		return null;
	}

	@Override
	public String getProviderDescription() {
		return NLS.bind(Messages.CdsFieldTopDownElementInfoProvider_ProviderDescription_xmsg, this.cdsViewName, this.field);
	}

}