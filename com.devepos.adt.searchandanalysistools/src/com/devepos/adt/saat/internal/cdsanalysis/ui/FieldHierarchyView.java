package com.devepos.adt.saat.internal.cdsanalysis.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

import com.devepos.adt.saat.internal.ICommandConstants;
import com.devepos.adt.saat.internal.IContextMenuConstants;
import com.devepos.adt.saat.internal.IDestinationProvider;
import com.devepos.adt.saat.internal.ObjectType;
import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.cdsanalysis.CdsFieldTopDownElementInfoProvider;
import com.devepos.adt.saat.internal.cdsanalysis.ICdsAnalysisConstants;
import com.devepos.adt.saat.internal.menu.MenuItemFactory;
import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.saat.internal.tree.IAdtObjectReferenceNode;
import com.devepos.adt.saat.internal.tree.ITreeNode;
import com.devepos.adt.saat.internal.tree.LazyLoadingFolderNode;
import com.devepos.adt.saat.internal.ui.RadioActionGroup;
import com.devepos.adt.saat.internal.util.AdtUtil;
import com.devepos.adt.saat.internal.util.IImages;

/**
 * View which consists of a {@link ViewForm} for the lower section of the
 * {@link FieldAnalysisView}
 *
 * @author stockbal
 */
public class FieldHierarchyView implements IDestinationProvider {
	static final String DIRECTION = "DIRECTION"; //$NON-NLS-1$
	static final String TOP_DOWN_ACTION = "topDown"; //$NON-NLS-1$
	static final String WHERE_USED_ACTION = "whereUsed"; //$NON-NLS-1$

	private RadioActionGroup actionToggleGroup;

	private final FieldAnalysisView parentView;
	private final ViewForm hierarchyViewerViewForm;
	private final PageBook pageBook;
	private final Composite noFieldSelectionComposite;
	private ITreeNode fieldNode;
	private final CLabel hierarchyViewerPaneLabel;
	private FieldHierarchyViewer hierarchyTreeViewer;
	private Map<String, FieldHierarchyViewerInput> fieldInputMap;
	private FieldHierarchyViewerInput currentFieldInput;
	private ObjectType currentInputObjectType;
	private String currentEntityName;
	private Action searchCalcFieldsAction;

	private IDestinationProvider destinationProvider;

	/**
	 * Creates new instance of the {@link FieldHierarchyView}
	 */
	public FieldHierarchyView(final FieldAnalysisView parentView, final Composite parent) {
		this.parentView = parentView;
		this.fieldInputMap = new HashMap<>();
		this.pageBook = new PageBook(parent, SWT.NONE);
		this.noFieldSelectionComposite = createNoFieldSelectionComposite(this.pageBook);
		this.hierarchyViewerViewForm = new ViewForm(this.pageBook, SWT.NONE);
		final Control hierarchyViewerControl = createHierarchyViewerControl(this.hierarchyViewerViewForm);
		configureFieldHierarchyTree(this.hierarchyTreeViewer);
		this.hierarchyViewerViewForm.setContent(hierarchyViewerControl);

		this.hierarchyViewerPaneLabel = new CLabel(this.hierarchyViewerViewForm, SWT.NONE);
		this.hierarchyViewerViewForm.setTopLeft(this.hierarchyViewerPaneLabel);

		final ToolBar toolbar = new ToolBar(this.hierarchyViewerViewForm, SWT.FLAT | SWT.WRAP);
		this.hierarchyViewerViewForm.setTopCenter(toolbar);
		final ToolBarManager fieldTbm = new ToolBarManager(toolbar);

		createToolbarActions();
		fillToolbar(fieldTbm);

		fieldTbm.update(true);

		this.pageBook.showPage(this.noFieldSelectionComposite);
	}

	/**
	 * Clears the cached nodes
	 */
	public void clearInputCache() {
		this.fieldInputMap.clear();
	}

	/**
	 * Returns the current input cache
	 *
	 * @return the current input cache
	 */
	public Map<String, FieldHierarchyViewerInput> getInputCache() {
		return this.fieldInputMap;
	}

	/**
	 * Updates the field to hierarchy viewer input cache
	 * 
	 * @param inputCache the field hierarchy viewer cache
	 */
	public void setInputCache(final Map<String, FieldHierarchyViewerInput> inputCache) {
		if (inputCache == null) {
			this.fieldInputMap = new HashMap<>();
		} else {
			this.fieldInputMap = inputCache;
		}
	}

	/**
	 * Sets the entity information whose fields should be analyzed
	 *
	 * @param entityName    the name of a database entity
	 * @param destinationId the destination id of the ABAP project
	 * @param entityType    the type of the database entity
	 */
	public void setEntityInformation(final String entityName, final IDestinationProvider destinationProvider,
		final ObjectType entityType) {
		this.currentInputObjectType = entityType;
		this.currentEntityName = entityName;
		this.destinationProvider = destinationProvider;
	}

	/**
	 * Sets the visibility of the view form which holds the hierarchy viewer
	 *
	 * @param visible if <code>true</code> the view will be made visible
	 */
	public void setVisible(final boolean visible) {
		if (visible) {
			this.pageBook.showPage(this.hierarchyViewerViewForm);
		} else {
			this.pageBook.showPage(this.noFieldSelectionComposite);
		}
	}

	/**
	 * Returns <code>true</code> if this view is visible
	 *
	 * @return <code>true</code> if this view is visible
	 */
	public boolean isVisible() {
		return this.hierarchyViewerViewForm.isVisible();
	}

	/**
	 * Updates the input of the tree viewer for a single field
	 *
	 * @param node the newly chosen field
	 */
	public void setFieldHierarchyInput(final ITreeNode node) {
		this.fieldNode = node;
		final String fieldName = node.getName();
		FieldHierarchyViewerInput input = this.fieldInputMap.get(fieldName);
		if (input == null) {
			// check if top down is possible
			// create new input
			LazyLoadingFolderNode topDownNode = null;
			if (this.parentView.uriDiscovery.isHierarchyAnalysisAvailable()
				&& this.currentInputObjectType == ObjectType.CDS_VIEW) {
				topDownNode = new LazyLoadingFolderNode(this.currentEntityName, this.currentEntityName,
					new CdsFieldTopDownElementInfoProvider(getDestinationId(), this.currentEntityName, fieldName),
					node.getParent().getImageId(), null, null);
				topDownNode.getProperties().put(ICdsAnalysisConstants.FIELD_PROP, node.getDisplayName());
			}
			input = new FieldHierarchyViewerInput(this.hierarchyTreeViewer, topDownNode, this.currentEntityName, fieldName, this);
			input.createWhereUsedNode();

			this.fieldInputMap.put(fieldName, input);
		}
		this.currentFieldInput = input;
		this.searchCalcFieldsAction.setChecked(input.isSearchCalcFieldsActive());
		this.actionToggleGroup.enableAction(TOP_DOWN_ACTION, input.getTopDownNode().hasContent());
		final boolean isTopDown = TOP_DOWN_ACTION.equals(this.actionToggleGroup.getToggledActionId());
		this.hierarchyTreeViewer.setInput(input, isTopDown);
		this.searchCalcFieldsAction.setEnabled(!isTopDown);
		updateToolbarLabel(isTopDown);
	}

	@Override
	public void setDestinationId(final String destinationId) {
		this.destinationProvider.setDestinationId(destinationId);
	}

	@Override
	public String getDestinationId() {
		return this.destinationProvider.getDestinationId();
	}

	@Override
	public String getSystemId() {
		return this.destinationProvider.getSystemId();
	}

	public StructuredViewer getViewer() {
		return this.hierarchyTreeViewer;
	}

	public void reloadFieldInput() {
		this.hierarchyTreeViewer.reloadInput(TOP_DOWN_ACTION.equals(this.actionToggleGroup.getToggledActionId()));
	}

	private void createToolbarActions() {
		this.actionToggleGroup = new RadioActionGroup();
		this.actionToggleGroup.addAction(TOP_DOWN_ACTION, Messages.FieldHierarchyViewer_FieldOriginModeButton_xtol,
			SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.FIELD_TOP_DOWN), true);
		this.actionToggleGroup.addAction(WHERE_USED_ACTION, Messages.FieldHierarchyViewer_FieldReferencesModeButton_xtol,
			SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.FIELD_WHERE_USED), false);
		this.actionToggleGroup.addActionToggledListener(actionId -> {
			final boolean isTopDown = TOP_DOWN_ACTION.equals(actionId);
			this.hierarchyTreeViewer.updateInput(isTopDown);
			updateToolbarLabel(isTopDown);
			this.searchCalcFieldsAction.setEnabled(!isTopDown);
		});
		this.searchCalcFieldsAction = new Action(Messages.FieldHierarchyView_CalculatedFieldsSearch_xtol, Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				if (FieldHierarchyView.this.currentFieldInput == null) {
					return;
				}
				FieldHierarchyView.this.currentFieldInput.setSearchCalcFields(isChecked());
				reloadFieldInput();
			}
		};
		this.searchCalcFieldsAction.setImageDescriptor(SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.FUNCTION));
	}

	private void fillToolbar(final ToolBarManager fieldTbm) {
		fieldTbm.add(this.searchCalcFieldsAction);
		fieldTbm.add(new Separator());
		this.actionToggleGroup.contributeToToolbar(fieldTbm);
	}

	/*
	 * Create composite for no selected field
	 */
	private Composite createNoFieldSelectionComposite(final PageBook pageBook) {
		final Composite composite = new Composite(pageBook, SWT.BACKGROUND);
		GridLayoutFactory.swtDefaults().applyTo(composite);

		final Label label = new Label(composite, SWT.LEAD | SWT.TOP | SWT.WRAP);
		label.setText(Messages.FieldHierarchyView_NoFieldSelected_xfld);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(5, SWT.DEFAULT).applyTo(label);
		return composite;
	}

	/*
	 * Create tree viewer for displaying the field hierarchy for a single field
	 */
	private Control createHierarchyViewerControl(final Composite parent) {
		final Composite hierarchyComposite = new Composite(parent, SWT.NONE);
		hierarchyComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		hierarchyComposite.setSize(100, 100);
		hierarchyComposite.setLayout(new FillLayout());
		this.hierarchyTreeViewer = new FieldHierarchyViewer(hierarchyComposite);
		return hierarchyComposite;
	}

	/*
	 * Updates the Tool Bar label
	 */
	private void updateToolbarLabel(final boolean topDown) {
		this.hierarchyViewerPaneLabel.setImage(SearchAndAnalysisPlugin.getDefault().getImage(this.fieldNode.getImageId()));
		final StringBuilder infoLabelText = new StringBuilder(
			this.currentInputObjectType != ObjectType.CDS_VIEW ? this.fieldNode.getDisplayName().toUpperCase()
				: this.fieldNode.getDisplayName());
		infoLabelText.append("   ["); //$NON-NLS-1$
		if (topDown) {
			infoLabelText.append(Messages.FieldHierarchyView_FieldOriginModeHeading_xfld);
		} else {
			infoLabelText.append(Messages.FieldHierarchyView_FieldReferencesModeHeading_xfld);
		}
		infoLabelText.append("]"); //$NON-NLS-1$
		this.hierarchyViewerPaneLabel.setText(infoLabelText.toString());
	}

	/*
	 * Perform some configuration tasks on the fields hierarchy viewer
	 */
	private void configureFieldHierarchyTree(final FieldHierarchyViewer hierarchyTreeViewer) {
		hierarchyTreeViewer.initContextMenu(menu -> {
			this.parentView.fillContextMenu(menu);
			if (menu.find(ICommandConstants.WHERE_USED_IN_CDS_ANALYSIS) != null) {
				MenuItemFactory.addCdsAnalyzerCommandItem(menu, IContextMenuConstants.GROUP_CDS_ANALYSIS,
					ICommandConstants.FIELD_ANALYSIS);
			}
			contributeToHierarchyViewerContextMenu(menu);
		}, null, null);
		// register field navigation as double click and ENTER action
		hierarchyTreeViewer.addDoubleClickListener(event -> {
			final IAdtObjectReferenceNode adtObjRefNode = getAdtObjRefFromSelection();
			if (adtObjRefNode == null) {
				return;
			}
			final String entityName = adtObjRefNode.getDisplayName();
			final String fieldName = adtObjRefNode.getPropertyValue(ICdsAnalysisConstants.FIELD_PROP);
			if (entityName == null || fieldName == null) {
				return;
			}
			AdtUtil.navigateToEntityColumn(entityName, fieldName, getDestinationId());
		});
	}

	/*
	 * Contributes actions to the context menu of fields hierarchy viewer
	 */
	private void contributeToHierarchyViewerContextMenu(final IMenuManager menu) {
		final IAdtObjectReferenceNode adtObjRefNode = getAdtObjRefFromSelection();
		if (adtObjRefNode == null) {
			return;
		}
		final String entityName = adtObjRefNode.getDisplayName();
		final String fieldName = adtObjRefNode.getPropertyValue(ICdsAnalysisConstants.FIELD_PROP);
		if (entityName == null || fieldName == null) {
			return;
		}

		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new Action(Messages.FieldHierarchyView_NavigateToFieldAction_xmit) {
			@Override
			public void run() {
				AdtUtil.navigateToEntityColumn(entityName, fieldName, getDestinationId());
			}
		});

	}

	/*
	 * Retrieves an ADT object reference node from the current selection or 'null'
	 */
	private IAdtObjectReferenceNode getAdtObjRefFromSelection() {
		final StructuredSelection selection = (StructuredSelection) this.hierarchyTreeViewer.getSelection();
		if (selection.isEmpty()) {
			return null;
		}
		final Object selected = selection.getFirstElement();
		if (!(selected instanceof IAdtObjectReferenceNode)) {
			return null;
		}
		return (IAdtObjectReferenceNode) selected;
	}
}
