package com.juju.app.navi;

import java.util.HashMap;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;


/**
 * Author: wyouflf
 * Date: 13-11-12
 */
public class MyDistanceUtil {
	
	private static int zoomLevel[] = {2000000,1000000,500000,200000,100000,
        50000,25000,20000,10000,5000,2000,1000,500,100,50,20,0}; 
	
    private MyDistanceUtil() {
    }
    
    public static LatLng entity2Baidu(Location location){
    	return new LatLng(location.getLat(), location.getLng());
    }
    
    private static double getDistanceFromXtoY(double lat_a,double lng_a,double lat_b,double lng_b){
    	double pk = 180 / 3.14169  ;
    	double a1 = lat_a / pk  ;
    	double a2 = lng_a / pk  ;
    	double b1 = lat_b / pk  ;
    	double b2 = lng_b / pk  ;
    	double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2)  ;
    	double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2)  ;
    	double t3 = Math.sin(a1) * Math.sin(b1)  ;
    	double tt = Math.acos(t1 + t2 + t3)  ;
		return 6366000 * tt ;
    }
    
    public static Location getCenterFromXtoY(Location loc_start,Location loc_end){
    	return getCenterFromXtoY(loc_start.getLat(),loc_start.getLng(),loc_end.getLat(),loc_end.getLng());
    }
    
    private static Location getCenterFromXtoY(double lat_a,double lng_a,double lat_b,double lng_b){
    	Location location = new Location();
    	location.setLat((lat_a+lat_b)/2);
    	location.setLng((lng_a+lng_b)/2);
    	return location;
    }
    
    private static int getZoomByDistance(int width,int height,double lat_a,double lng_a,double lat_b,double lng_b){
    	double distance = MyDistanceUtil.getDistanceFromXtoY(lat_a, lng_a, lat_b, lng_b);
    	return MyDistanceUtil.getZoomByDistance(width,height,distance);
    }
    
    private static int getZoomByDistance(int width,int height,Location loc_start,Location loc_end){
    	return getZoomByDistance(width,height,loc_start.getLat(),loc_start.getLng(),loc_end.getLat(),loc_end.getLng());
    }
    
    private static int getZoomByDistance(int width,int height,double distance){
    	HashMap<Integer, Double> map = new HashMap<Integer,Double>(){{
    		put(17, 600d);
    		put(16, 1200d);
    		put(15, 2400d);
    		put(14, 4800d);
    		put(13, 9600d);
    		put(12, 19200d);
    		put(11, 38400d);
    		put(10, 76800d);
    		put(9, 153600d);
    		put(8, 307200d);
    		put(7, 614400d);
    		put(6, 1228800d);
    		put(5, 2457600d);
    	}};
    	
    	double mindis = width>height?height:width;
    	double scale = width/400d;
    	
    	if (distance<map.get(17)) {
			return 17;
		}else if (map.get(17)*scale<=distance && distance<map.get(16)*scale){
			return 16;
		}else if (map.get(16)*scale<=distance && distance<map.get(15)*scale){
			return 15;
		}else if (map.get(15)*scale<=distance && distance<map.get(14)*scale){
			return 14;
		}else if (map.get(14)*scale<=distance && distance<map.get(13)*scale){
			return 13;
		}else if (map.get(13)*scale<=distance && distance<map.get(12)*scale){
			return 12;
		}else if (map.get(12)*scale<=distance && distance<map.get(11)*scale){
			return 11;
		}else if (map.get(11)*scale<=distance && distance<map.get(10)*scale){
			return 10;
		}else if (map.get(10)*scale<=distance && distance<map.get(9)*scale){
			return 9;
		}else if (map.get(9)*scale<=distance && distance<map.get(8)*scale){
			return 8;
		}else if (map.get(8)*scale<=distance && distance<map.get(7)*scale){
			return 7;
		}else if (map.get(7)*scale<=distance && distance<map.get(6)*scale){
			return 6;
		}else if (map.get(6)*scale<=distance && distance<map.get(5)*scale){
			return 5;
		}else{
			return 4;
		}
    }
    
    public static Location getLocationByStr(String coor,String address){
    	Location loc = getLocationByStr(coor);
    	if (loc!=null) {
			loc.setAddress(address);
		}
    	return loc;
    }
    
    public static Location getLocationByStr(String coor){
    	Location location = new Location();
    	try {
			String[] temp = coor.split(",");
			location.setLng(Double.parseDouble(temp[0]));
			location.setLat(Double.parseDouble(temp[1]));
			return location;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static String getUrl(int width,int height,Location loc_start,Location loc_end){
    	if (loc_start!=null && loc_end!=null) {
    		return MyDistanceUtil.getUrl(width, height, loc_start.getLat(), loc_start.getLng(), loc_end.getLat(), loc_end.getLng());
    	}else{
			return "error";
		}
    }
    
    public static String getUrl(int width,int height,String start_coor,String end_coor){
    	Location loc_start = MyDistanceUtil.getLocationByStr(start_coor);
    	Location loc_end = MyDistanceUtil.getLocationByStr(end_coor);
    	if (loc_start!=null && loc_end!=null) {
    		return MyDistanceUtil.getUrl(width, height, loc_start.getLat(), loc_start.getLng(), loc_end.getLat(), loc_end.getLng());
		}else{
			return "error";
		}
    }
    
    private static int getZoomForMap(double lat_a,double lng_a,double lat_b,double lng_b){
    	return getZoomForMap(new LatLng(lat_a,lng_a), new LatLng(lat_b,lng_b));
    }
    
    private static int getZoomForMap(LatLng start,LatLng end){
    	int distance = (int)DistanceUtil.getDistance(start, end);
	    int i;
	    for(i=0;i<17;i++){
	        if(zoomLevel[i]<distance){
	            break;
	        }
	    }
	    int zoom = i+4;
	    return zoom;
    }
    
    public static int getZoomForMap(Location loc_start,Location loc_end){
    	return getZoomForMap(MyDistanceUtil.entity2Baidu(loc_start),MyDistanceUtil.entity2Baidu(loc_end));
    }
    
    public static String getUrl(int width,int height,double lat_a,double lng_a,double lat_b,double lng_b){
    	String url = "http://api.map.baidu.com/staticimage?";
	    	url += "width=##width##&height=##height##";
	    	url += "&center=##lng##,##lat##";
	    	url += "&zoom=##zoom##";
	    	url += "&markers=##lng_a##,##lat_a##|##lng_b##,##lat_b##";
	    	url += "&markerStyles=l,S|l,E";
    	
	    try {
	    	Location center = MyDistanceUtil.getCenterFromXtoY(lat_a, lng_a, lat_b, lng_b);
//	    	int zoom = MyDistanceUtil.getZoomByDistance(width,height,lat_a, lng_a, lat_b, lng_b);
	    	int zoom = MyDistanceUtil.getZoomForMap(lat_a, lng_a, lat_b, lng_b);
	    	
	    	url = url.replaceAll("##width##", width+"");
	    	url = url.replaceAll("##height##", height+"");
	    	url = url.replaceAll("##lng##", center.getLng()+"");
	    	url = url.replaceAll("##lat##", center.getLat()+"");
	    	url = url.replaceAll("##zoom##", zoom+"");
	    	url = url.replaceAll("##lng_a##", lng_a+"");
	    	url = url.replaceAll("##lat_a##", lat_a+"");
	    	url = url.replaceAll("##lng_b##", lng_b+"");
	    	url = url.replaceAll("##lat_b##", lat_b+"");
	    	return url;
		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
    }
}
