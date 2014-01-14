package com.routon.calendar;

public class GridInfo {  
  
	private int ImageID;
    private String GC;
    private String LC;
    private String MD;
    private int color = 0xFFFFFFFF;
  
    public GridInfo(){
    	
    }
      
    public GridInfo(String GC, String LC, String MD, int ImageID, int color) {  
        super();  
        this.GC = GC; 
        this.LC = LC;
        this.MD = MD;
        this.ImageID = ImageID;
        this.color = color;
    } 
  
    public String getGC() {  
        return GC;  
    }  
    
    public String getLC(){
    	return LC;
    }
    
    public String getMD(){
    	return MD;
    }
    
    public int getImageID(){
    	return ImageID;
    }
    
    public int getColor(){
    	return color;
    }
  
    public void setGC(String GC) {  
        this.GC = GC;  
   }  

    public void setLC(String LC) {  
        this.LC = LC;  
   }  
    
    public void setMD(String MD) {  
        this.MD = MD;  
   }  
    
    public void setImageID(int ImageID) {  
        this.ImageID = ImageID;  
   }  
    
   public void setColor(int color){
	   this.color = color;
   }
   
   public GridInfo clone()
   {
	   GridInfo gridInfoNew = new GridInfo();
	   gridInfoNew.setColor(this.getColor());
	   gridInfoNew.setGC(this.getGC());
	   gridInfoNew.setImageID(this.getImageID());
	   gridInfoNew.setLC(this.getLC());
	   gridInfoNew.setMD(this.getMD());
	   
	   return gridInfoNew;
   }
}
