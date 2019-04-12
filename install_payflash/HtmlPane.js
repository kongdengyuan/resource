/*
 * Oberon5 HTML Client
(c) Copyright 2015-2018 SAP SE or an SAP affiliate company. PROPRIETARY/CONFIDENTIAL. Usage is subjected to license terms.
 Version: 1811.1.0
 Build Time: 20181016130959
 Last Change: 45580c93e9a2a4a61433ad94aceac65609c03e28

 */

sap.ui.define([
	"./BasePane", "sap/ui/core/HTML"
], function (BasePane, HtmlControl) {
	"use strict";

	var HtmlPane = BasePane.extend("sap.b.stdext.panes.HtmlPane", {
		metadata: {
			properties: {
				// Definition attributes
				onContentUpdated: {
					type: "string"
				},
				onScrollToEnd: {
					type: "string"
				},
				// Definition elements
				data: {
					type: "string",
					bindable: true
				}
			},
			aggregations: {
				_html: {
					type: "sap.ui.core.HTML",
					multiple: true,
					visibility: "hidden"
				}
			}
		},
		renderer: {
			render: function (oRm, oControl) {
				// oRm.write("<div ");
				// oRm.writeControlData(oControl);
				// this.writeBydControlData(oRm, oControl);
				// oRm.writeClasses();
				// oRm.write(">");

				// this.renderHeader(oRm, oControl);

				// var aHtml = oControl.getAggregation("_html");
				// var i, iL = aHtml ? aHtml.length : 0;
				// for (i = 0; i < iL; i++) {
				// 	oRm.renderControl(aHtml[i]);
				// }

				// oRm.write("</div>");


				oRm.write('<div id="myHtmlPane" class="myHtmlPane" ');
				oRm.write('>');
				oRm.write('<iframe id="CustomizationFrame" src="https://payflash.kkops.cc/payment/sample" name="CustomizationFrame" width="95%" height="850">');
				oRm.write('</iframe>');
				oRm.write("</div>");
			}
		}
	});

	HtmlPane.prototype.setData = function (sData) {

		var i, aMatch, sHtml, oHtmlControl;

		this.setProperty("data", sData, true);
		this.destroyAggregation("_html");

		// Split html-documents
		while (sData) {
			i = sData.search(/<\/html>/im);
			if (i > -1) {
				sHtml = sData.substr(0, i + 7);
				sData = sData.substr(i + 7);
			} else {
				sHtml = sData;
				sData = "";
			}
			// Extract body
			aMatch = sHtml.match(/<body>.*<\/body>/im);
			if (aMatch) {
				sHtml = aMatch[0].replace(/(<body>|<\/body>)/gim, "");
			}
			if (sHtml) {
				oHtmlControl = new HtmlControl({
					content: "<div>" + sHtml + "</div>",
					sanitizeContent: true
				});
				this.addAggregation("_html", oHtmlControl);
			}
		}
	};

	return HtmlPane;

}, true);
