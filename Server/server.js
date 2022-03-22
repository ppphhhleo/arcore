const myserver = require("http");
const urlib = require("url");
const fs = require("fs");
const readline=require("readline"); 
var mysql=require("mysql");
var db_config={
	host:'localhost',
	user:'root',
	password:'zl123',
	database:'farm_info'
}

const R=6371e3;

function distance(lat1, lng1, lat2, lng2) {
	var radLat1 = lat1 * Math.PI / 180.0;
	var radLat2 = lat2 * Math.PI / 180.0;
	var a = radLat1 - radLat2;
	var b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0;
	var s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
	s = s * 6378.137;
	s = Math.round(s * 10000) / 10;
	return s
}


myserver.createServer(function (req,res){
    console.log(req.url);   
    var params ={};
	var numbers=[];
    if (req.url.indexOf('?')!==-1)
    {
        params = req.url.split("?");
        console.log(params);
        params = params[1].split("&");
        for(var $i=0;$i<params.length;$i++)
        {
            var myitem = params[$i].split("=");
			numbers.push(myitem[1]);
            //res.write(myitem[0]+"==========="+myitem[1]+"\n");
        }
		var lat=parseFloat(numbers[0]);
		var lon=parseFloat(numbers[1]);
		var radius=parseInt(numbers[2]);
		var minlat=lat-radius/R*180/Math.PI;
		var maxlat=lat+radius/R*180/Math.PI;
		var minlon=lon-radius/R*180/Math.PI/Math.cos(lat*Math.PI/180);
		var maxlon=lon+radius/R*180/Math.PI/Math.cos(lat*Math.PI/180);
		var results=[];
		const sql='select * from farminfo where latitude between '+minlat+' and '+maxlat+' and longitude between '+minlon+' and '+maxlon;
		var connection=mysql.createConnection(db_config);
		connection.query(sql,function(err,result){
			if(err){
				console.log(err);
			}else{
				var data=JSON.stringify(result);
				var json=JSON.parse(data);
				for(var i in json){
					var latitude=json[i].latitude;
					var longitude=json[i].longitude;
					if(distance(lat,lon,latitude,longitude)<=radius){
						results.push(json[i]);
					}
				}
				console.log(results);	
				res.write(JSON.stringify(results));		
				res.end();
			}
				
		});
		connection.end();
		
    }
    else
    {
        res.write(req.url);
		res.end();
    }
   
}).listen(8090);
