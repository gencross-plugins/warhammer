package com.mrprez.gencross.impl.warhammer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.PropertiesList;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.history.ProportionalHistoryFactory;
import com.mrprez.gencross.listener.custom.CustomAfterAddPropertyListener;
import com.mrprez.gencross.listener.custom.CustomAfterDeletePropertyListener;
import com.mrprez.gencross.listener.custom.CustomBeforeAddPropertyListener;
import com.mrprez.gencross.value.IntValue;
import com.mrprez.gencross.value.Value;

public class Warhammer extends Personnage {
	private Set<String> choiceProperties;
	
	
	
	protected void init(){
		for(Property property : properties.values()){
			findChoiceProperty(property);
		}
	}
	
	public Warhammer clone() throws CloneNotSupportedException{
		Warhammer clone = (Warhammer) super.clone();
		if(choiceProperties!=null){
			clone.choiceProperties = new HashSet<String>();
			clone.choiceProperties.addAll(choiceProperties);
		}
		return clone;
	}
	
	
	
	protected void findChoiceProperty(Property property){
		if(property.getSubProperties()==null){
			return;
		}
		for(Property subProperty : property.getSubProperties()){
			if(subProperty.getName().matches("Choix[ ][0-9]*")){
				if(choiceProperties==null){
					choiceProperties = new HashSet<String>();
				}
				choiceProperties.add(subProperty.getAbsoluteName());
			}else{
				findChoiceProperty(subProperty);
			}
		}
	}
	
	protected void addChoiceProperty(Property motherProperty, Collection<String> options) throws SecurityException, NoSuchMethodException{
		if(choiceProperties==null){
			choiceProperties = new HashSet<String>();
		}
		Property choiceProperty = new Property("Choix "+(choiceProperties.size()+1), motherProperty);
		choiceProperties.add(choiceProperty.getAbsoluteName());
		motherProperty.getSubProperties().add(choiceProperty);
		choiceProperty.addSubPropertiesList(false, false);
		choiceProperty.getSubProperties().setCanRemoveElement(true);
		choiceProperty.setRemovable(false);
		for(String optionName : options){
			Property choiceOption = motherProperty.getSubProperties().getOptions().get(optionName);
			if(choiceOption==null){
				choiceOption = motherProperty.getSubProperties().getDefaultProperty().clone();
			}
			choiceOption = choiceOption.clone();
			choiceOption.setName(optionName);
			choiceOption.setEditable(false);
			choiceOption.setRemovable(true);
			choiceProperty.getSubProperties().addOptionProperty(choiceOption);
		}
		this.addBeforeAddPropertyListener(new CustomBeforeAddPropertyListener(this, "checkChooseProperty", choiceProperty.getAbsoluteName()+"#[^#]*"));
		this.addAfterAddPropertyListener(new CustomAfterAddPropertyListener(this, "chooseProperty", choiceProperty.getAbsoluteName()+"#[^#]*"));
		this.addAfterDeletePropertyListener(new CustomAfterDeletePropertyListener(this, "removeChoosenProperty", choiceProperty.getAbsoluteName()+"#[^#]*"));
		
	}
	
	public Boolean checkChooseProperty(Property newProperty){
		if(newProperty.getValue()==null){
			Property choiceProperty = (Property) newProperty.getOwner();
			PropertiesList propertiesList = ((Property)choiceProperty.getOwner()).getSubProperties();
			for(Property property : propertiesList){
				if(property.getFullName().equals(newProperty.getFullName())){
					actionMessage = "Vous avez déjà cette propriété.";
					return Boolean.FALSE;
				}
				if(property.getName().matches("Choix[ ][0-9]*")){
					if(!property.getSubProperties().isEmpty()){
						if(property.getSubProperties().get(0).getFullName().equals(newProperty.getFullName())){
							actionMessage = "Vous avez déjà cette propriété.";
							return Boolean.FALSE;
						}
					}
				}
			}
		}
		return Boolean.TRUE;
	}
	
	public void chooseProperty(Property newProperty){
		((Property)newProperty.getOwner()).getSubProperties().setFixe(true);
		((Property)newProperty.getOwner()).getSubProperties().setCanRemoveElement(true);
	}
	
	public void removeChoosenProperty(Property oldProperty){
		((Property)oldProperty.getOwner()).getSubProperties().setFixe(false);
	}
	
	protected void calculateChoices(){
		if(choiceProperties==null){
			init();
		}
		for(String choicePropertyName : choiceProperties){
			Property property = getProperty(choicePropertyName);
			if(property.getSubProperties().isEmpty()){
				errors.add("Vous devez choisir: "+property.getAbsoluteName().replace("#", "/"));
			}
		}
	}
	
	private void resolveChoice(){
		for(String choiceName : choiceProperties){
			Property propertyChoice = getProperty(choiceName);
			PropertiesList propertiesList = ((Property)propertyChoice.getOwner()).getSubProperties();
			Property choosen = propertyChoice.getSubProperties().get(0);
			String optionName = choosen.getFullName();
			if(propertiesList.get(optionName)==null){
				propertiesList.add(propertiesList.getOptions().get(optionName));
			}
			Property property = propertiesList.get(optionName);
			property.setRemovable(false);
			if(property.getSubProperties()!=null && property.getSubProperty("niveau")!=null){
				Property niveau = property.getSubProperty("niveau");
				int niveauValue = niveau.getValue().getInt();
				niveau.setValue(Value.createValue(niveauValue+1));
				niveau.setMin();
				niveau.setMax();
				recalculateCompetence(property);
			}
			((Property)propertyChoice.getOwner()).getSubProperties().remove(propertyChoice);
		}
		choiceProperties.clear();
	}
	
	public void calculate(){
		super.calculate();
		if(phase.equals("Choix Race")){
			calculateRace();
		}else if(phase.equals("Profil")){
			calculateProfil();
		}else if(phase.equals("Base")){
			calculateChoices();
			calculateBase();
		}else if(phase.equals("Choix carrière")){
			calculateChoixCarriere();
		}else if(phase.equals("Choix talents/compétences")){
			calculateChoices();
			calculateChoixTalents();
		}
		
		
	}
	
	private void calculateRace(){
		if(getProperty("Race").getValue().getString().isEmpty()){
			errors.add("Vous devez choisir une race.");
		}
	}
	
	private void calculateProfil(){
		//Property profil = getProperty("Profil");
		// TODO décommenter
		/*if(profil.getSubProperty("CC").getSubProperty("base").getValue().getInt()==profil.getSubProperty("CC").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("CT").getSubProperty("base").getValue().getInt()==profil.getSubProperty("CT").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("F").getSubProperty("base").getValue().getInt()==profil.getSubProperty("F").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("E").getSubProperty("base").getValue().getInt()==profil.getSubProperty("E").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("Ag").getSubProperty("base").getValue().getInt()==profil.getSubProperty("Ag").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("Int").getSubProperty("base").getValue().getInt()==profil.getSubProperty("Int").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("FM").getSubProperty("base").getValue().getInt()==profil.getSubProperty("FM").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("Soc").getSubProperty("base").getValue().getInt()==profil.getSubProperty("Soc").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("B").getSubProperty("base").getValue().getInt()==profil.getSubProperty("B").getSubProperty("base").getMin().getInt()
				|| profil.getSubProperty("PD").getValue().getInt()==profil.getSubProperty("PD").getMin().getInt()){
			errors.add("Vous devez remplir votre profil.");
		}*/
	}
	
	private void calculateBase(){
		PropertiesList talentList = getProperty("Talents").getSubProperties();
		Integer tailleLim = null;
		String race = getProperty("Race").getValue().toString();
		if(race.equals("Halfling")){
			tailleLim = 4;
		}else if(race.equals("Humain")){
			tailleLim = 2;
		}
		if(tailleLim!=null){
			if(talentList.size()!=tailleLim){
				errors.add("Il vous reste "+(tailleLim-talentList.size())+" talent à ajouter");
			}
		}
		for(Property talent : talentList){
			if(talent.getName().startsWith("Choix:") && talent.getValue().getString().isEmpty()){
				errors.add("Vous devez choisir un talent: "+talent.getName().replace("Choix: ", ""));
			}
		}
	}
	
	public void calculateChoixCarriere(){
		if(getProperty("Carrière").getValue().getString().isEmpty()){
			errors.add("Vous devez choisir une carrière.");
		}
	}
	
	private void calculateChoixTalents(){
		for(Property talent : getProperty("Talents").getSubProperties()){
			if(talent.getName().startsWith("Choix:")){
				if(talent.getValue().getString().isEmpty()){
					errors.add("Vous devez choisir un talent: "+talent.getName().replace("Choix:", ""));
				}
			}
		}
	}
	
	
	public void passToProfilPhase() throws Exception{
		getProperty("Race").setEditable(false);
		String race = getProperty("Race").getValue().toString();
		
		// Initialisation du profil
		Property profil = getProperty("Profil");
		for(Property property : profil.getSubProperties()){
			String key = "profil."+race+"."+property.getName();
			String minKey = key+".min";
			String maxKey = key+".max";
			if(appendix.containsKey(key)){
				IntValue value= Value.createValue(Integer.parseInt(appendix.getProperty(key)));
				property.getSubProperty("base").setValue(value);
			}else if(appendix.containsKey(minKey)){
				IntValue min = Value.createValue(Integer.parseInt(appendix.getProperty(minKey)));
				IntValue max = Value.createValue(Integer.parseInt(appendix.getProperty(maxKey)));
				if(property.getSubProperties()!=null){
					property = property.getSubProperty("base");
				}
				property.setEditable(true);
				property.setValue(min);
				property.setMin();
				property.setMax(max);
				this.formulaManager.impactModificationFor(property.getAbsoluteName(), this);
			}
		}
		
		
		
	}
	
	public void passToBase() throws Exception{
		String race = getProperty("Race").getValue().toString();
		
		// Ajout de compétences
		PropertiesList competenceList = getProperty("Compétences").getSubProperties();
		String compPrefix = "trait."+race+".competence.";
		for(int i=0; appendix.containsKey(compPrefix+i); i++){
			String competenceNom = appendix.getProperty(compPrefix+i);
			if(competenceNom.equals("Choix")){
				this.addChoiceProperty(competenceList.getOwner(), new TreeSet<String>(appendix.getSubMap(compPrefix+i+".Choix.").values()));
			}else if(competenceList.get(competenceNom)!=null){
				Property competence = competenceList.get(competenceNom);
				Property niveau = competence.getSubProperty("niveau");
				niveau.setValue(new IntValue(1));
				niveau.setMax();
				niveau.setMin();
				recalculateCompetence(competence);
			}else{
				Property competence = competenceList.getOptions().get(competenceNom);
				Property niveau = competence.getSubProperty("niveau");
				niveau.setValue(new IntValue(1));
				niveau.setMax();
				niveau.setMin();
				competence.setName(competenceNom);
				competenceList.add(competence);
				recalculateCompetence(competence);
			}
		}
		
		// Ajout de talent
		PropertiesList talentList = getProperty("Talents").getSubProperties();
		String talentPrefix = "trait."+race+".talent.";
		for(int i=0; appendix.containsKey(talentPrefix+i); i++){
			String nomTalent = appendix.getProperty(talentPrefix+i);
			if(nomTalent.equals("Choix") && appendix.containsKey(talentPrefix+i+".Choix.0")){
				addChoiceProperty(talentList.getOwner(), new TreeSet<String>(appendix.getSubMap(talentPrefix+i+".Choix.").values()));
			}else if(nomTalent.equals("Choix")){
				addChoiceProperty(talentList.getOwner(), new TreeSet<String>(appendix.getSubMap("talent.").values()));
			}else{
				Property talent = talentList.getOptions().get(nomTalent);
				this.addPropertyToMotherProperty(talent);
				talentList.get(nomTalent).setRemovable(false);
			}
		}
		
	}
	
	public void passToChoixCarriere() throws Exception{
		resolveChoice();
		
		for(Property competence : getProperty("Compétences").getSubProperties()){
			recalculateCompetence(competence);
		}
		
		getProperty("Profil").setEditableRecursivly(false);
		
		Collection<String> carriereList = appendix.getSubMap("carriere.base."+getProperty("Race").getValue().getString()+".").values();
		getProperty("Carrière").setOptions(carriereList.toArray(new String[]{}));
		getProperty("Carrière").setEditable(true);
	}
	
	public void passToChoixTalentCompetence() throws SecurityException, NoSuchMethodException{
		getProperty("Carrière").setEditable(false);
		String carriere = getProperty("Carrière").getValue().getString();
		String appendixPrefix = findCarriereKey(carriere);
		
		// Talents
		PropertiesList talentList = getProperty("Talents").getSubProperties();
		for(int i=0; appendix.containsKey(appendixPrefix+".talent."+i); i++){
			String nomTalent = appendix.getProperty(appendixPrefix+".talent."+i);
			if(nomTalent.contains(" ou ")){
				addChoiceProperty(talentList.getOwner(), Arrays.asList(nomTalent.split(" ou ")));
			}else{
				Property talent = talentList.getOptions().get(nomTalent).clone();
				talentList.add(talent);
			}
		}
		
		// Compétences
		PropertiesList competenceList = getProperty("Compétences").getSubProperties();
		for(int i=0; appendix.containsKey(appendixPrefix+".competence."+i); i++){
			String nomCompetence = appendix.getProperty(appendixPrefix+".competence."+i);
			if(nomCompetence.contains(" ou ")){
				addChoiceProperty(competenceList.getOwner(), Arrays.asList(nomCompetence.split(" ou ")));
			}else if(competenceList.get(nomCompetence)!=null){
				Property competence = competenceList.get(nomCompetence);
				Property niveau = competence.getSubProperty("niveau");
				niveau.setValue(Value.createValue(1+niveau.getValue().getInt()));
				niveau.setMin();
				niveau.setMax();
			}else{
				Property competence = competenceList.getOptions().get(nomCompetence).clone();
				competenceList.add(competence);
				competence.setRemovable(false);
			}
		}
		
		// Profil
		for(Property carac : getProperty("Profil").getSubProperties()){
			if(appendix.containsKey(appendixPrefix+"."+carac.getFullName())){
				Property niveau = carac.getSubProperty("avance");
				niveau.setMin();
				int profilBonus = Integer.parseInt(appendix.getProperty(appendixPrefix+"."+carac.getFullName()));
				niveau.setMax(Value.createValue(niveau.getValue().getInt()+profilBonus));
			}else{
				carac.setMin();
				carac.setMax();
			}
		}
		
	}
	
	public void passToInit(){
		
		// Enregistrement des compétences non prises.
		Set<String> notChoosenComp = new HashSet<String>();
		for(Property competenceProperty : getProperty("Compétences").getSubProperties()){
			if(choiceProperties.contains(competenceProperty.getAbsoluteName())){
				notChoosenComp.addAll(competenceProperty.getSubProperties().getOptions().keySet());
				notChoosenComp.remove(competenceProperty.getSubProperties().get(0).getFullName());
			}
		}
		
		// Enregistrement des talents non pris
		Set<String> notChoosenTalent = new HashSet<String>();
		for(Property talentProperty : getProperty("Talents").getSubProperties()){
			if(choiceProperties.contains(talentProperty.getAbsoluteName())){
				notChoosenTalent.addAll(talentProperty.getSubProperties().getOptions().keySet());
				notChoosenTalent.remove(talentProperty.getSubProperties().get(0).getFullName());
			}
		}
		
		// Suppression des choix
		resolveChoice();
		
		// Gestion des compétences non prises.
		PropertiesList competenceList = getProperty("Compétences").getSubProperties();
		for(String notChoosenOptionName : notChoosenComp){
			Property competence;
			if(competenceList.get(notChoosenOptionName)!=null){
				competence = competenceList.get(notChoosenOptionName);
			}else{
				competence = competenceList.getOptions().get(notChoosenOptionName);
				competence.setRemovable(true);
			}
			Property niveau = competence.getSubProperty("niveau");
			niveau.setMax(Value.createValue(niveau.getMax().getInt()+1));
		}
		
		
		// Gestion des talents non pris
		PropertiesList talentList = getProperty("Talents").getSubProperties();
		talentList.getOptions().clear();
		for(String notChoosenOptionName : notChoosenTalent){
			Property talent = talentList.getDefaultProperty().clone();
			talent.setName(notChoosenOptionName);
			talent.setRemovable(true);
			talentList.addOptionProperty(talent);
		}
		
		// Libération des 100 points d'init
		getPointPools().get("Init").setToEmpty(true);
		getProperty("Compétences").setHistoryFactory(new ProportionalHistoryFactory("Init", 100));
		for(Property carac : getProperty("Profil").getSubProperties()){
			if(carac.getSubProperties()!=null){
				carac.getSubProperty("avance").setEditable(true);
			}
		}
		getProperty("Talents").getSubProperties().setFixe(false);
		getProperty("Compétences").getSubProperties().setFixe(false);
		
	}
	
	public void passToCarriere(){
		for(Property carac : getProperty("Profil").getSubProperties()){
			if(carac.getSubProperties()!=null){
				carac.getSubProperty("avance").setMin();
				carac.getSubProperty("avance").getHistoryFactory().setPointPool("Experience");
			}
		}
		getProperty("Compétences").getHistoryFactory().setPointPool("Experience");
		for(Property talent : getProperty("Talents").getSubProperties()){
			talent.setRemovable(false);
		}
		for(Property talentOption : getProperty("Talents").getSubProperties().getOptions().values()){
			talentOption.getHistoryFactory().setPointPool("Experience");
		}
		
	}
	
	public Boolean removeCompetence(Property competence){
		if(competence.getSubProperty("niveau")==null){
			return Boolean.TRUE;
		}
		return Boolean.valueOf(competence.getSubProperty("niveau").getValue().getInt()>0);
	}
	
	public void changeCompetenceNiveau(Property property, Value oldValue){
		recalculateCompetence((Property) property.getOwner());
	}
	
	public void changeProfil(Property niveauProperty, Value oldValue){
		for(Property competence : getProperty("Compétences").getSubProperties()){
			recalculateCompetence(competence);
		}
	}
	
	private String findCarriereKey(String carriere){
		for(Object key : appendix.keySet()){
			String stringKey = (String) key;
			if(appendix.getProperty(stringKey).equals(carriere)){
				if(stringKey.startsWith("carriere.desc.")){
					return stringKey;
				}
			}
		}
		return null;
	}
	
	private void recalculateCompetence(Property competence){
		int niveau = competence.getSubProperty("niveau").getValue().getInt();
		String nomCarac = competence.getSubProperty("Carac").getValue().getString();
		Property carac = getProperty("Profil").getSubProperty(nomCarac);
		if(niveau==0){
			competence.setValue(new IntValue(carac.getValue().getInt()/2));
		}else if(niveau==1){
			competence.setValue(new IntValue(carac.getValue().getInt()));
		}else if(niveau==2){
			competence.setValue(new IntValue(carac.getValue().getInt()+10));
		}else{
			competence.setValue(new IntValue(carac.getValue().getInt()+20));
		}
	}
	
	
	
	

}
