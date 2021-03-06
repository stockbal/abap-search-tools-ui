package com.devepos.adt.saat.internal.cdsanalysis;

import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.devepos.adt.base.elementinfo.IElementInfo;
import com.devepos.adt.base.elementinfo.IElementInfoCollection;
import com.devepos.adt.base.elementinfo.IElementInfoProvider;
import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.messages.Messages;

public class CdsTopDownElementInfoProvider implements IElementInfoProvider {
    private final String cdsViewName;
    private final String destinationId;

    public CdsTopDownElementInfoProvider(final String destinationId, final String cdsViewName) {
        this.cdsViewName = cdsViewName;
        this.destinationId = destinationId;
    }

    @Override
    public List<IElementInfo> getElements() {
        final boolean loadAssociations = SearchAndAnalysisPlugin.getDefault()
            .getPreferenceStore()
            .getBoolean(ICdsAnalysisPreferences.TOP_DOWN_LOAD_ASSOCIATIONS);
        final IElementInfo cdsTopDownInfo = CdsAnalysisServiceFactory.createCdsAnalysisService()
            .loadTopDownAnalysis(cdsViewName, loadAssociations, destinationId);
        if (cdsTopDownInfo != null) {
            return ((IElementInfoCollection) cdsTopDownInfo).getChildren();
        }
        return null;
    }

    @Override
    public String getProviderDescription() {
        return NLS.bind(Messages.CdsAnalysis_TopDownAnalysisProviderDescription_xmsg, cdsViewName);
    }

}
