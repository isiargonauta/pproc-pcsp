package es.unizar.contsem.codice.parser;

import java.util.Iterator;

import org.apache.jena.riot.RDFDataMgr;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceF;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

import es.unizar.contsem.vocabulary.CPV;
import es.unizar.contsem.vocabulary.GR;
import es.unizar.contsem.vocabulary.ORG;
import es.unizar.contsem.vocabulary.PC;
import es.unizar.contsem.vocabulary.PPROC;
import es.unizar.contsem.vocabulary.S;

public class CodiceToPprocParser {

	private static String BASE_URI_CONTRATO = "http://contsem.unizar.es/datos/sector-publico/contrato/";
	private static String BASE_URI_ORGANIZATION = "http://contsem.unizar.es/datos/sector-publico/organization/";

	/**
	 * 
	 * @param model
	 *            RDF Jena Model
	 * @param document
	 *            CODICE XML document
	 */
	public static void parseCodiceXML(Model model, Document document) {

		String altString = null, altString2 = null;
		Element altElement = null;

		// pproc:Contract rdf:type (1)
		String contractResourceURI = BASE_URI_CONTRATO + document.getRootElement().elementText("UUID");
		Resource contractResource = model.createResource(contractResourceURI);
		contractResource.addProperty(RDF.type, PPROC.Contract);

		// pproc:Contract dcterms:title
		if ((altString = document.getRootElement().element("ProcurementProject").elementText("Name")) != null)
			contractResource.addProperty(DCTerms.title, altString);

		// pproc:Contract dcterms:description
		if ((altString = document.getRootElement().element("ProcurementProject").elementText("Description")) != null)
			contractResource.addProperty(DCTerms.description, altString);

		// pproc:Contract rdf:type (2)
		// TODO parseo independiente de versi�n (parseo del .gc)
		if ((altString = document.getRootElement().element("ProcurementProject").elementText("TypeCode")) != null)
			switch (altString) {
			case "1":
				contractResource.addProperty(RDF.type, PPROC.SuppliesContract);
				break;
			case "2":
				contractResource.addProperty(RDF.type, PPROC.ServicesContract);
				break;
			case "3":
				contractResource.addProperty(RDF.type, PPROC.WorksContract);
				break;
			case "21":
				contractResource.addProperty(RDF.type, PPROC.PublicServicesManagementContract);
				break;
			case "31":
				contractResource.addProperty(RDF.type, PPROC.PublicWorksConcessionContract);
				break;
			case "40":
				contractResource.addProperty(RDF.type, PPROC.PublicPrivatePartnershipContract);
				break;
			case "7":
				contractResource.addProperty(RDF.type, PPROC.SpecialAdministrativeContract);
				break;
			case "8":
				contractResource.addProperty(RDF.type, PPROC.PrivateContract);
				break;
			case "50":
				// Contrato patrimonial, no hay equivalencia en PPROC
			}

		// pproc:Contract rdf:type (3)
		// TODO parseo independiente de versi�n (parseo del .gc)
		if (model.containsResource(PPROC.SuppliesContract))
			if ((altString = document.getRootElement().element("ProcurementProject").elementText("SubTypeCode")) != null)
				switch (altString) {
				case "1":
					contractResource.addProperty(RDF.type, PPROC.RentContract);
					break;
				case "2":
					contractResource.addProperty(RDF.type, PPROC.BuyContract);
				}

		// pproc:Contract dcterms:identifier
		if ((altString = document.getRootElement().elementText("ContractFolderID")) != null)
			contractResource.addProperty(DCTerms.identifier, altString);

		// pproc:Contract pc:contractingAuthority
		for (Iterator iter = document.getRootElement().element("ContractingParty").element("Party")
				.elementIterator("PartyIdentification"); iter.hasNext();) {
			altElement = (Element) iter.next();
			if (altElement.element("ID").attributeValue("schemeName").equals("ID_PLATAFORMA"))
				altString = altElement.elementText("ID");
			else if (altElement.element("ID").attributeValue("schemeName").equals("NIF"))
				altString2 = altElement.elementText("ID");
		}
		String organizationResourceURI = BASE_URI_ORGANIZATION + altString;
		if (!model.containsResource(ResourceFactory.createResource(organizationResourceURI))) {

			// org:Organization rdf:type
			Resource organizationResource = model.createResource(organizationResourceURI);
			organizationResource.addProperty(RDF.type, ORG.Organization);

			// org:Organization dcterms:title
			if ((altString = document.getRootElement().element("ContractingParty").element("Party")
					.element("PartyName").elementText("Name")) != null)
				organizationResource.addProperty(DCTerms.title, altString);

			// org:Organization dcterms:identifier
			if (altString2 != null)
				organizationResource.addProperty(DCTerms.identifier, altString2);

			// org:Organization org:hasSite
			Resource placeResource = model.createResource(organizationResourceURI + "/Place");
			placeResource.addProperty(RDF.type, S.Place);
			organizationResource.addProperty(ORG.hasSite, placeResource);

			// s:Place s:address
			Resource postalAddressResource = model.createResource(organizationResourceURI + "/PostalAddress");
			postalAddressResource.addProperty(RDF.type, S.PostalAddress);
			placeResource.addProperty(S.address, postalAddressResource);
			if ((altString = document.getRootElement().element("ContractingParty").element("Party")
					.element("PostalAddress").elementText("CityName")) != null)
				postalAddressResource.addProperty(S.addressLocality, altString);
			if ((altString = document.getRootElement().element("ContractingParty").element("Party")
					.element("PostalAddress").elementText("PostalZone")) != null)
				postalAddressResource.addProperty(S.postalCode, altString);
			if ((altString = document.getRootElement().element("ContractingParty").element("Party")
					.element("PostalAddress").element("AddressLine").elementText("Line")) != null)
				postalAddressResource.addProperty(S.streetAddress, altString);
			if ((altString = document.getRootElement().element("ContractingParty").element("Party")
					.element("PostalAddress").element("Country").elementText("IdentificationCode")) != null)
				postalAddressResource.addProperty(S.addressCountry, altString);

			// org:Organization s:telephone
			if ((altString = document.getRootElement().element("ContractingParty").element("Party").element("Contact")
					.elementText("Telephone")) != null)
				postalAddressResource.addProperty(S.telephone, altString);

			// org:Organization s:faxNumber
			if ((altString = document.getRootElement().element("ContractingParty").element("Party").element("Contact")
					.elementText("Telefax")) != null)
				postalAddressResource.addProperty(S.faxNumber, altString);

			// org:Organization s:email
			if ((altString = document.getRootElement().element("ContractingParty").element("Party").element("Contact")
					.elementText("ElectronicMail")) != null)
				postalAddressResource.addProperty(S.email, altString);

		}
		contractResource.addProperty(PC.contractingAuthority, organizationResourceURI);

		// pproc:Contract pproc:delegatingAuthority
		altString2 = null;
		if (document.getRootElement().element("OriginatorCustomerParty") != null) {
			for (Iterator iter = document.getRootElement().element("OriginatorCustomerParty").element("Party")
					.elementIterator("PartyIdentification"); iter.hasNext();) {
				altElement = (Element) iter.next();
				if (altElement.element("ID").attributeValue("schemeName").equals("ID_PLATAFORMA"))
					altString = altElement.elementText("ID");
				else if (altElement.element("ID").attributeValue("schemeName").equals("NIF"))
					altString2 = altElement.elementText("ID");

			}
			organizationResourceURI = BASE_URI_ORGANIZATION + altString;
			if (!model.containsResource(ResourceFactory.createResource(organizationResourceURI))) {

				// org:Organization rdf:type
				Resource organizationResource = model.createResource(organizationResourceURI);
				organizationResource.addProperty(RDF.type, ORG.Organization);

				// org:Organization dcterms:title
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("PartyName").elementText("Name")) != null)
					organizationResource.addProperty(DCTerms.title, altString);

				// org:Organization dcterms:identifier
				if (altString2 != null)
					organizationResource.addProperty(DCTerms.identifier, altString2);

				// org:Organization org:hasSite
				Resource placeResource = model.createResource(organizationResourceURI + "/Place");
				placeResource.addProperty(RDF.type, S.Place);
				organizationResource.addProperty(ORG.hasSite, placeResource);

				// s:address s:PostalAddress
				Resource postalAddressResource = model.createResource(organizationResourceURI + "/PostalAddress");
				postalAddressResource.addProperty(RDF.type, S.PostalAddress);
				placeResource.addProperty(S.address, postalAddressResource);
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("PostalAddress").elementText("CityName")) != null)
					postalAddressResource.addProperty(S.addressLocality, altString);
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("PostalAddress").elementText("PostalZone")) != null)
					postalAddressResource.addProperty(S.postalCode, altString);
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("PostalAddress").element("AddressLine").elementText("Line")) != null)
					postalAddressResource.addProperty(S.streetAddress, altString);
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("PostalAddress").element("Country").elementText("IdentificationCode")) != null)
					postalAddressResource.addProperty(S.addressCountry, altString);

				// org:Organization s:telephone
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("Contact").elementText("Telephone")) != null)
					postalAddressResource.addProperty(S.telephone, altString);

				// org:Organization s:faxNumber
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("Contact").elementText("Telefax")) != null)
					postalAddressResource.addProperty(S.faxNumber, altString);

				// org:Organization s:email
				if ((altString = document.getRootElement().element("OriginatorCustomerParty").element("Party")
						.element("Contact").elementText("ElectronicMail")) != null)
					postalAddressResource.addProperty(PPROC.delegatingAuthority, altString);

			}
			contractResource.addProperty(PC.contractingAuthority, organizationResourceURI);
		}

		// pproc:Contract pproc:contractTemporalConditions
		if (document.getRootElement().element("ProcurementProject").element("PlannedPeriod") != null) {
			Resource ctcResource = model.createResource(contractResourceURI + "/ContractTemporalConditions");
			ctcResource.addProperty(RDF.type, PPROC.ContractTemporalConditions);
			contractResource.addProperty(PPROC.contractTemporalConditions, ctcResource);

			// pproc:ContractTemporalConditions pproc:estimatedDuration
			if ((altString = document.getRootElement().element("ProcurementProject").element("PlannedPeriod")
					.elementText("DurationMeasure")) != null
					&& (altString2 = document.getRootElement().element("ProcurementProject").element("PlannedPeriod")
							.element("DurationMeasure").attributeValue("unitCode")) != null)
				switch (altString2) {
				case "DAY":
					ctcResource.addProperty(PPROC.estimatedDuration, "P" + altString + "D");
					break;
				case "MON":
					ctcResource.addProperty(PPROC.estimatedDuration, "P" + altString + "M");
					break;
				case "ANN":
					ctcResource.addProperty(PPROC.estimatedDuration, "P" + altString + "Y");
					break;
				}
			// TODO pproc:ContractTemporalConditions pproc:estimatedEndDate
			// (falta mirar como es udt:DateType)
		}

		// pproc:Contract pproc:contractEconomicConditions (1)
		if (document.getRootElement().element("ProcurementProject").element("BudgetAmount") != null) {
			Resource cecResource = model.createResource(contractResourceURI + "/ContractEconomicConditions");
			cecResource.addProperty(RDF.type, PPROC.ContractEconomicConditions);
			contractResource.addProperty(PPROC.contractEconomicConditions, cecResource);

			// pproc:ContractEconomicConditions pproc:estimatedValue
			if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
					.elementText("EstimatedOverallContractAmount")) != null) {
				Resource priceResource = model.createResource(contractResourceURI
						+ "/ContractEconomicConditions/EstimatedValue");
				priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
				priceResource.addProperty(GR.hasCurrencyValue, altString);
				priceResource.addProperty(GR.valueAddedTaxIncluded, "true");
				if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
						.element("EstimatedOverallContractAmount").attributeValue("currencyID")) != null)
					priceResource.addProperty(GR.hasCurrency, altString);
				cecResource.addProperty(PPROC.estimatedValue, priceResource);
			}

			// pproc:ContractEconomicConditions pproc:budgetPrice (1)
			if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
					.elementText("TotalAmount")) != null) {
				Resource priceResource = model.createResource(contractResourceURI
						+ "/ContractEconomicConditions/TotalAmount");
				priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
				priceResource.addProperty(GR.hasCurrencyValue, altString);
				priceResource.addProperty(GR.valueAddedTaxIncluded, "true");
				if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
						.element("TotalAmount").attributeValue("currencyID")) != null)
					priceResource.addProperty(GR.hasCurrency, altString);
				cecResource.addProperty(PPROC.budgetPrice, priceResource);
			}

			// pproc:ContractEconomicConditions pproc:budgetPrice (2)
			if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
					.elementText("TaxExclusiveAmount")) != null) {
				Resource priceResource = model.createResource(contractResourceURI
						+ "/ContractEconomicConditions/TaxExclusiveAmount");
				priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
				priceResource.addProperty(GR.hasCurrencyValue, altString);
				priceResource.addProperty(GR.valueAddedTaxIncluded, "false");
				if ((altString = document.getRootElement().element("ProcurementProject").element("BudgetAmount")
						.element("TaxExclusiveAmount").attributeValue("currencyID")) != null)
					priceResource.addProperty(GR.hasCurrency, altString);
				cecResource.addProperty(PPROC.budgetPrice, priceResource);
			}
		}

		// pproc:Contract pproc:contractEconomicConditions (2)
		if (document.getRootElement().element("ProcurementProject").element("RequiredFeeAmount") != null) {
			Resource cecResource = model.createResource(contractResourceURI + "/ContractEconomicConditions");
			cecResource.addProperty(RDF.type, PPROC.ContractEconomicConditions);
			contractResource.addProperty(PPROC.contractEconomicConditions, cecResource);

			// pproc:ContractEconomicConditions pproc:feePrice
			if ((altString = document.getRootElement().element("ProcurementProject").elementText("RequiredFeeAmount")) != null) {
				Resource priceResource = model.createResource(contractResourceURI
						+ "/ContractEconomicConditions/EstimatedValue");
				priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
				priceResource.addProperty(GR.hasCurrencyValue, altString);
				priceResource.addProperty(GR.valueAddedTaxIncluded, "true");
				if ((altString = document.getRootElement().element("ProcurementProject").element("RequiredFeeAmount")
						.attributeValue("currencyID")) != null)
					priceResource.addProperty(GR.hasCurrency, altString);
				cecResource.addProperty(PPROC.feePrice, priceResource);
			}
		}

		// pproc:Contract pproc:contractProcedureSpecifications
		if (document.getRootElement().element("TenderingProcess") != null) {
			Resource cpeResource = model.createResource(contractResourceURI + "/ContractProcedureSpecifications");
			cpeResource.addProperty(RDF.type, PPROC.ContractProcedureSpecifications);
			contractResource.addProperty(PPROC.contractProcedureSpecifications, cpeResource);

			// pproc:ContractProcedureSpecifications pproc:urgencyType
			// TODO parseo independiente de versi�n (parseo del .gc)
			if ((altString = document.getRootElement().element("TenderingProcess").elementText("UrgencyCode")) != null)
				switch (altString) {
				case "1":
					cpeResource.addProperty(PPROC.urgencyType, PPROC.Regular);
					break;
				case "2":
					cpeResource.addProperty(PPROC.urgencyType, PPROC.Express);
					break;
				case "3":
					cpeResource.addProperty(PPROC.urgencyType, PPROC.Emergency);
				}

			// pproc:ContractProcedureSpecifications pproc:procedureType
			// TODO parseo independiente de versi�n (parseo del .gc)
			if ((altString = document.getRootElement().element("TenderingProcess").elementText("ProcedureCode")) != null)
				switch (altString) {
				case "1":
					cpeResource.addProperty(PPROC.procedureType, PPROC.RegularOpen);
					break;
				case "2":
					cpeResource.addProperty(PPROC.procedureType, PPROC.Restricted);
					break;
				case "3":
					cpeResource.addProperty(PPROC.procedureType, PPROC.NegotiatedWithoutPublicity);
					cpeResource.addProperty(PPROC.procedureType, PPROC.Negotiated);
					break;
				case "4":
					cpeResource.addProperty(PPROC.procedureType, PPROC.NegotiatedWithPublicity);
					cpeResource.addProperty(PPROC.procedureType, PPROC.Negotiated);
					break;
				case "5":
					cpeResource.addProperty(PPROC.procedureType, PPROC.CompetitiveDialogue);
					break;
				case "100":
					// Normas internas, no definido en PPROC
				}

			// pproc:Contract rdf:type (4)
			// TODO parseo independiente de versi�n (parseo del .gc)
			if ((altString = document.getRootElement().element("TenderingProcess").elementText("ContractingSystemCode")) != null)
				switch (altString) {
				case "1":
					contractResource.addProperty(RDF.type, PPROC.FrameworkConclusionContract);
					break;
				case "2":
					contractResource.addProperty(RDF.type, PPROC.DynamicPurchasingSystemConclusionContract);
					break;
				case "3":
					contractResource.addProperty(RDF.type, PPROC.FrameworkDerivativeContract);
					break;
				case "4":
					contractResource.addProperty(RDF.type, PPROC.DynamicPurchasingSystemDerivativeContract);
				}

			// pproc:Contract pproc:frameworAgreement
			if (document.getRootElement().element("TenderingProcess").element("FrameworAgreement") != null
					&& (model.containsResource(PPROC.FrameworkConclusionContract) || model
							.containsResource(PPROC.DynamicPurchasingSystemConclusionContract))) {
				Resource frameworkResource = model.createResource(contractResourceURI + "/FrameworkAgreement");
				frameworkResource.addProperty(RDF.type, PPROC.FrameworkAgreement);

				// pproc:FrameworkResource pproc:maxNumberOfOperators
				if ((altString = document.getRootElement().element("TenderingProcess").element("FrameworAgreement")
						.elementText("MaximumOperatorsQuantity")) != null)
					frameworkResource.addProperty(PPROC.maxNumberOfOperators, altString);

				// pproc:FrameworkResource pproc:estimatedDuration
				if ((altString = document.getRootElement().element("TenderingProcess").element("FrameworAgreement")
						.elementText("DurationMeasure")) != null
						&& (altString2 = document.getRootElement().element("TenderingProcess")
								.element("FrameworAgreement").element("DurationMeasure").attributeValue("unitCode")) != null)
					switch (altString2) {
					case "DAY":
						frameworkResource.addProperty(PPROC.estimatedDuration, "P" + altString + "D");
						break;
					case "MON":
						frameworkResource.addProperty(PPROC.estimatedDuration, "P" + altString + "M");
						break;
					case "ANN":
						frameworkResource.addProperty(PPROC.estimatedDuration, "P" + altString + "Y");
						break;
					}
				// TODO pproc:FrameworkResource pproc:estimatedEndDate
				// (falta mirar como es udt:DateType)
				contractResource.addProperty(PPROC.frameworkAgreement, frameworkResource);
			}
		}

		// pproc:Contract pproc:contractObject
		if ((altString = document.getRootElement().element("ProcurementProject")
				.element("RequiredCommodityClassification").elementText("ItemClassificationCode")) != null) {
			Resource objectResource = model.createResource(contractResourceURI + "/ContractObject");
			objectResource.addProperty(RDF.type, PPROC.ContractObject);

			// pproc:ContractObject pproc:mainObject
			objectResource.addProperty(PPROC.mainObject, CPV.code(altString));

			// pproc:ContractObject pproc:provision
			if (document.getRootElement().element("ProcurementProject").element("RequestForTenderLine") != null) {
				for (Iterator iter = document.getRootElement().element("ProcurementProject")
						.elementIterator("RequestForTenderLine"); iter.hasNext();) {
					altElement = (Element) iter.next();
					Resource offeringResource = model.createResource(contractResourceURI + "/ContractObject/Offering_"
							+ altElement.elementText("ID"));
					offeringResource.addProperty(RDF.type, GR.Offering);

					// gr:Offering dcterms:title
					if ((altString = altElement.element("Item").elementText("Name")) != null)
						offeringResource.addProperty(DCTerms.title, altString);

					// gr:Offering dcterms:description
					if ((altString = altElement.element("Item").elementText("Description")) != null)
						offeringResource.addProperty(DCTerms.description, altString);

					// gr:Offering gr:hasEligibleQuantity
					if ((altString = altElement.elementText("Quantity")) != null) {
						Resource quantityResource = model.createResource(contractResourceURI
								+ "/ContractObject/Offering_" + altElement.elementText("ID") + "/QuantitativeValue");
						quantityResource.addProperty(RDF.type, GR.QuantitativeValue);

						// gr:QuantitativeValue gr:hasValue
						quantityResource.addProperty(GR.hasValue, altString);

						// gr:QuantitativeValue gr:hasUnitOfMeasurement
						if ((altString = altElement.element("Quantity").attributeValue("unitCode")) != null)
							quantityResource.addProperty(GR.hasUnitOfMeasurement, altString);

						offeringResource.addProperty(GR.hasEligibleQuantity, quantityResource);
					}

					// gr:Offering gr:hasPriceSpecification (1)
					if ((altString = altElement.elementText("MaximumTaxExclusiveAmount")) != null) {
						Resource priceResource = model.createResource(contractResourceURI + "/ContractObject/Offering_"
								+ altElement.elementText("ID") + "/MaximumTaxExclusiveAmount");
						priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
						priceResource.addProperty(GR.hasCurrencyValue, altString);
						priceResource.addProperty(GR.valueAddedTaxIncluded, "false");
						if ((altString = altElement.element("MaximumTaxExclusiveAmount").attributeValue("currencyID")) != null)
							priceResource.addProperty(GR.hasCurrency, altString);
						offeringResource.addProperty(GR.hasPriceSpecification, priceResource);
					}

					// gr:Offering gr:hasPriceSpecification (2)
					if ((altString = altElement.elementText("MaximumTaxInclusiveAmount")) != null) {
						Resource priceResource = model.createResource(contractResourceURI + "/ContractObject/Offering_"
								+ altElement.elementText("ID") + "/MaximumTaxInclusiveAmount");
						priceResource.addProperty(RDF.type, PPROC.BundlePriceSpecification);
						priceResource.addProperty(GR.hasCurrencyValue, altString);
						priceResource.addProperty(GR.valueAddedTaxIncluded, "true");
						if ((altString = altElement.element("MaximumTaxInclusiveAmount").attributeValue("currencyID")) != null)
							priceResource.addProperty(GR.hasCurrency, altString);
						offeringResource.addProperty(GR.hasPriceSpecification, priceResource);
					}

					// gr:Offering gr:hasPriceSpecification (3)
					if ((altString = altElement.element("RequiredItemLocationQuantity").element("Price")
							.elementText("PriceAmount")) != null) {
						Resource priceResource = model.createResource(contractResourceURI + "/ContractObject/Offering_"
								+ altElement.elementText("ID") + "/UnitPrice");
						priceResource.addProperty(RDF.type, GR.UnitPriceSpecification);
						priceResource.addProperty(GR.hasCurrencyValue, altString);
						priceResource.addProperty(GR.valueAddedTaxIncluded, "true");
						if ((altString = altElement.element("RequiredItemLocationQuantity").element("Price")
								.element("PriceAmount").attributeValue("currencyID")) != null)
							priceResource.addProperty(GR.hasCurrency, altString);
						offeringResource.addProperty(GR.hasPriceSpecification, priceResource);

						// gr:UnitPriceSpecification gr:hasEligibleQuantity
						altString = null;
						altString2 = null;
						if (((altString = altElement.element("RequiredItemLocationQuantity").elementText(
								"MinimumQuantity")) != null)
								|| ((altString2 = altElement.element("RequiredItemLocationQuantity").elementText(
										"MaximumQuantity")) != null)) {
							Resource quantityResource = model.createResource(contractResourceURI
									+ "/ContractObject/Offering_" + altElement.elementText("ID")
									+ "/UnitPrice/QuantitativeValue");
							quantityResource.addProperty(RDF.type, GR.QuantitativeValue);

							// gr:QuantitativeValue gr:hasMinValue
							if (altString != null) {
								quantityResource.addProperty(GR.hasMinValue, altString);

								// gr:QuantitativeValue gr:hasUnitOfMeasurement
								if ((altString = altElement.element("RequiredItemLocationQuantity")
										.element("MinimumQuantity").attributeValue("unitCode")) != null)
									quantityResource.addProperty(GR.hasUnitOfMeasurement, altString);
							}

							// gr:QuantitativeValue gr:hasMaxValue
							if (altString2 != null) {
								quantityResource.addProperty(GR.hasMaxValue, altString);

								// gr:QuantitativeValue gr:hasUnitOfMeasurement
								if ((altString = altElement.element("RequiredItemLocationQuantity")
										.element("MaximumQuantity").attributeValue("unitCode")) != null)
									quantityResource.addProperty(GR.hasUnitOfMeasurement, altString);
							}
							priceResource.addProperty(GR.hasEligibleQuantity, quantityResource);
						}
					}
					objectResource.addProperty(PPROC.provision, offeringResource);
				}
			}
			contractResource.addProperty(PPROC.contractObject, objectResource);
		}

		// pproc:Contract pproc:tenderersRequirements
		if (document.getRootElement().element("TenderingTerms").element("TendererQualificationRequest") != null) {
			Resource tenderersRequirementsResource = model.createResource(contractResourceURI
					+ "/TenderersRequirements");
			tenderersRequirementsResource.addProperty(RDF.type, PPROC.TenderersRequirements);

			// pproc:TenderersRequirements pproc:requiredClassification
			if (document.getRootElement().element("TenderingTerms").element("TendererQualificationRequest")
					.element("RequiredBusinessClassificationScheme") != null
					&& (altString = document.getRootElement().element("TenderingTerms")
							.element("TendererQualificationRequest").element("RequiredBusinessClassificationScheme")
							.elementText("CodeValue")) != null) {
				tenderersRequirementsResource.addProperty(PPROC.requiredClassification, altString);
			}

			// pproc:TenderersRequirements proc:requiredEconomicAndFinancialStanding
			if ((altString = document.getRootElement().element("TenderingTerms")
					.element("TendererQualificationRequest").element("FinancialEvaluationCriteria")
					.elementText("Description")) != null)
				tenderersRequirementsResource.addProperty(PPROC.requiredEconomicAndFinancialStanding, altString);

			// pproc:TenderersRequirements pproc:requiredTechnicalAndProfessionalAbility
			if ((altString = document.getRootElement().element("TenderingTerms")
					.element("TendererQualificationRequest").element("TechnicalEvaluationCriteria")
					.elementText("Description")) != null)
				tenderersRequirementsResource.addProperty(PPROC.requiredTechnicalAndProfessionalAbility, altString);

			if (document.getRootElement().element("TenderingTerms").element("TendererQualificationRequest")
					.element("SpecificTendererRequirement") != null)
				for (Iterator iter = document.getRootElement().element("TenderingTerms")
						.element("TendererQualificationRequest").elementIterator("SpecificTendererRequirement"); iter
						.hasNext();) {
					altElement = (Element) iter.next();
					tenderersRequirementsResource.addProperty(PPROC.otherAbilityRequisites,
							altElement.element("RequirementTypeCode").attributeValue("name"));
				}
			contractResource.addProperty(PPROC.tenderersRequirements, tenderersRequirementsResource);
		}

		// pproc:Contract pc:awardCriteriaCombination
		if (document.getRootElement().element("TenderingTerms").element("AwardingTerms") != null
				&& document.getRootElement().element("TenderingTerms").element("AwardingTerms")
						.element("AwardingCriteria") != null) {
			Resource criteriaCombinationResource = model.createResource(contractResourceURI
					+ "/AwardCriteriaCombination");
			criteriaCombinationResource.addProperty(RDF.type, PC.AwardCriteriaCombination);

			// pc:AwardCriteriaCombination dcterms:description
			// TODO parseo independiente de versi�n (parseo del .gc)
			if ((altString = document.getRootElement().element("TenderingTerms").element("AwardingTerms")
					.elementText("WeightingAlgorithmCode")) != null)
				switch (altString) {
				case "1":
					criteriaCombinationResource.addProperty(DCTerms.description,
							"Algoritmo de ponderaci�n: Ponderaci�n lineal");
					break;
				case "2":
					criteriaCombinationResource.addProperty(DCTerms.description, "Algoritmo de ponderaci�n: Promedio");
					break;
				case "3":
					criteriaCombinationResource.addProperty(DCTerms.description, "Algoritmo de ponderaci�n: Topsis");
					break;
				case "4":
					criteriaCombinationResource.addProperty(DCTerms.description,
							"Algoritmo de ponderaci�n: Lexicogr�fico");
				}

			// pc:AwardCriteriaCombination pc:awardCriterion
			for (Iterator iter = document.getRootElement().element("TenderingTerms").element("AwardingTerms")
					.elementIterator("AwardingCriteria"); iter.hasNext();) {
				altElement = (Element) iter.next();
				Resource criterionResource = model.createResource(contractResourceURI
						+ "/AwardCriteriaCombination/Criterion" + altElement.elementText("ID"));
				criterionResource.addProperty(RDF.type, PC.AwardCriterion);

				// pc:AwardCriterion rdf:type
				// TODO parseo independiente de versi�n (parseo del .gc)
				if ((altString = altElement.elementText("AwardingCriteriaTypeCode")) != null)
					switch (altString) {
					case "SUBJ":
						criterionResource.addProperty(RDF.type, PPROC.SubjectiveAwardCriterion);
						break;
					case "OBJ":
						criterionResource.addProperty(RDF.type, PPROC.ObjectiveAwardCriterion);
					}

				// pc:AwardCriterion pc:criterionName
				if ((altString = altElement.elementText("Description")) != null)
					criterionResource.addProperty(PC.criterionName, altString);

				// pc:AwardCriterion pc:criterionWeight
				if ((altString = altElement.elementText("WeightNumeric")) != null)
					criterionResource.addProperty(PC.criterionWeight, altString);

				// pc:AwardCriterion pproc:criterionEvaluationMode
				if ((altString = altElement.elementText("CalculationExpression")) != null)
					criterionResource.addProperty(PPROC.criterionEvaluationMode, altString);

				// pc:AwardCriterion pproc:criterionMaxAndMinScores
				altString2 = "";
				if ((altString = altElement.elementText("MinimumQuantity")) != null)
					altString2 += "Cantidad m�nima: " + altString + ". ";
				if ((altString = altElement.elementText("MaximumQuantity")) != null)
					altString2 += "Cantidad m�xima: " + altString + ". ";
				if ((altString = altElement.elementText("MinimumAmount")) != null)
					altString2 += "Importe m�nimo: " + altString + ". ";
				if ((altString = altElement.elementText("MaximumAmount")) != null)
					altString2 += "Importe m�ximo: " + altString + ". ";
				if (altString2 != "")
					criterionResource.addProperty(PPROC.criterionMaxAndMinScores, altString2);

				criteriaCombinationResource.addProperty(PC.awardCriterion, criterionResource);
			}
			contractResource.addProperty(PC.awardCriteriaCombination, criteriaCombinationResource);
		}

		// pproc:ContractProcedureSpecifications pproc:contractAddionalObligations
		if (document.getRootElement().element("TenderingTerms") != null) {
			Resource cpeResource = model.createResource(contractResourceURI + "/ContractProcedureSpecifications");
			cpeResource.addProperty(RDF.type, PPROC.ContractProcedureSpecifications);
			contractResource.addProperty(PPROC.contractProcedureSpecifications, cpeResource);
			Resource addionalObligationsResource = model.createResource(contractResourceURI
					+ "/ContractAdditionalObligations");
			addionalObligationsResource.addProperty(RDF.type, PPROC.ContractAdditionalObligations);

			// pproc:ContractAdditionalObligations pproc:finalFinancialGuarantee
			// pproc:ContractAdditionalObligations pproc:finalFinancialGuaranteeDuration
			// pproc:ContractAdditionalObligations pproc:provisionalFinancialGuarantee
			// pproc:ContractAdditionalObligations pproc:otherGuarantee
			if (document.getRootElement().element("TenderingTerms").element("RequiredFinancialGuarantee") != null) {
				for (Iterator iter = document.getRootElement().element("TenderingTerms")
						.elementIterator("RequiredFinancialGuarantee"); iter.hasNext();) {
					altElement = (Element) iter.next();
					if ((altString = altElement.elementText("GuaranteeTypeCode")) != null) {
						switch (altString) {
						case "1":
							if (altElement.element("AmountRate") != null)
								addionalObligationsResource.addProperty(PPROC.finalFinancialGuarantee,
										altElement.elementText("AmountRate"));
							if (altElement.element("ConstitutionPeriod") != null) {
								altString = altElement.element("ConstitutionPeriod").elementText("DurationMeasure");
								altString2 = altElement.element("ConstitutionPeriod").element("DurationMeasure")
										.attributeValue("unitCode");
								switch (altString2) {
								case "DAY":
									addionalObligationsResource.addProperty(PPROC.finalFinancialGuaranteeDuration, "P"
											+ altString + "D");
									break;
								case "MON":
									addionalObligationsResource.addProperty(PPROC.finalFinancialGuaranteeDuration, "P"
											+ altString + "M");
									break;
								case "ANN":
									addionalObligationsResource.addProperty(PPROC.finalFinancialGuaranteeDuration, "P"
											+ altString + "Y");
									break;
								}
							}
							break;
						case "2":
							if (altElement.element("AmountRate") != null)
								addionalObligationsResource.addProperty(PPROC.provisionalFinancialGuarantee,
										altElement.elementText("AmountRate"));
							break;
						case "3":
							altString = "Garant�a especial";
							if (altElement.element("Description") != null)
								altString += " | " + altElement.elementText("Description");
							if (altElement.element("AmountRate") != null)
								altString += " | Porcentaje: " + altElement.elementText("AmountRate");
							if (altElement.element("LiabilityAmount") != null)
								altString += " | Importe: " + altElement.elementText("LiabilityAmount");
							addionalObligationsResource.addProperty(PPROC.otherGuarantee, altString);
						}
					}
				}
			}

			// pproc:ContractAdditionalObligations pproc:advertisementAmount
			if (document.getRootElement().element("TenderingTerms").element("MaximumAdvertisementAmount") != null) {
				altString2 = "Gastos m�ximos de publicidad: ";
				altString2 += document.getRootElement().element("TenderingTerms")
						.elementText("MaximumAdvertisementAmount");
				altString2 += " "
						+ document.getRootElement().element("TenderingTerms").element("MaximumAdvertisementAmount")
								.attributeValue("currencyID");
				addionalObligationsResource.addProperty(PPROC.advertisementAmount, altString2);
			}

			cpeResource.addProperty(PPROC.contractAdditionalObligations, addionalObligationsResource);
			contractResource.addProperty(PPROC.contractProcedureSpecifications, cpeResource);
		}

		// pproc:ContractProcedureSpecifications pproc:tenderInformationProvider (1)
		if (document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty") != null) {
			Resource cpeResource = model.createResource(contractResourceURI + "/ContractProcedureSpecifications");
			Resource informationProviderResource = model.createResource(contractResourceURI
					+ "/AdditionalInformationProvider");
			informationProviderResource.addProperty(RDF.type, PPROC.InformationProvider);

			// pproc:InformationProvider s:location
			Resource placeResource = model.createResource(informationProviderResource + "/Place");
			placeResource.addProperty(RDF.type, S.Place);
			informationProviderResource.addProperty(S.location, placeResource);

			// s:Place s:name
			if ((altString = document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty")
					.element("PartyName").elementText("Name")) != null)
				placeResource.addProperty(S.name, altString);

			// s:Place s:address
			Resource postalAddressResource = model.createResource(informationProviderResource + "/PostalAddress");
			postalAddressResource.addProperty(RDF.type, S.PostalAddress);
			placeResource.addProperty(S.address, postalAddressResource);
			if ((altString = document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty")
					.element("PostalAddress").elementText("CityName")) != null)
				postalAddressResource.addProperty(S.addressLocality, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty")
					.element("PostalAddress").elementText("PostalZone")) != null)
				postalAddressResource.addProperty(S.postalCode, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty")
					.element("PostalAddress").element("AddressLine").elementText("Line")) != null)
				postalAddressResource.addProperty(S.streetAddress, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("AdditionalInformationParty")
					.element("PostalAddress").element("Country").elementText("IdentificationCode")) != null)
				postalAddressResource.addProperty(S.addressCountry, altString);

			// pproc:InformationProvider pproc:estimatedEndDate
			if ((altElement = document.getRootElement().element("TenderingProcess")
					.element("DocumentAvailabilityPeriod")) != null)
				informationProviderResource.addProperty(PPROC.estimatedEndDate, altElement.elementText("EndDate"));

			cpeResource.addProperty(PPROC.tenderInformationProvider, informationProviderResource);
		}

		// pproc:ContractProcedureSpecifications pproc:tenderInformationProvider (2)
		if (document.getRootElement().element("TenderingTerms").element("DocumentProviderParty") != null) {
			Resource cpeResource = model.createResource(contractResourceURI + "/ContractProcedureSpecifications");
			Resource informationProviderResource = model.createResource(contractResourceURI + "/DocumentProviderParty");
			informationProviderResource.addProperty(RDF.type, PPROC.InformationProvider);

			// pproc:InformationProvider s:location
			Resource placeResource = model.createResource(informationProviderResource + "/Place");
			placeResource.addProperty(RDF.type, S.Place);
			informationProviderResource.addProperty(S.location, placeResource);

			// s:Place s:name
			if ((altString = document.getRootElement().element("TenderingTerms").element("DocumentProviderParty")
					.element("PartyName").elementText("Name")) != null)
				placeResource.addProperty(S.name, altString);

			// s:Place s:address
			Resource postalAddressResource = model.createResource(informationProviderResource + "/PostalAddress");
			postalAddressResource.addProperty(RDF.type, S.PostalAddress);
			placeResource.addProperty(S.address, postalAddressResource);
			if ((altString = document.getRootElement().element("TenderingTerms").element("DocumentProviderParty")
					.element("PostalAddress").elementText("CityName")) != null)
				postalAddressResource.addProperty(S.addressLocality, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("DocumentProviderParty")
					.element("PostalAddress").elementText("PostalZone")) != null)
				postalAddressResource.addProperty(S.postalCode, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("DocumentProviderParty")
					.element("PostalAddress").element("AddressLine").elementText("Line")) != null)
				postalAddressResource.addProperty(S.streetAddress, altString);
			if ((altString = document.getRootElement().element("TenderingTerms").element("DocumentProviderParty")
					.element("PostalAddress").element("Country").elementText("IdentificationCode")) != null)
				postalAddressResource.addProperty(S.addressCountry, altString);

			cpeResource.addProperty(PPROC.tenderInformationProvider, informationProviderResource);
		}

		//

		model.write(System.out, "Turtle");
	}
	// TODO a�adir los datatype a las propiedades data
	// TODO control de errores de parseo para cuando se lance contra toda la bbdd

}
