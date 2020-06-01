// function callHtml(fnname, data) {
//     eval(fnname + "(data)")
// }
console.log('哈哈哈')
var userId;
var dataUrl='';
var serverUrl;
var accessKey;
var accessToken = '';
// CallFunc('getUserConfig','')
// // GetUserConfig('{}')
//         var bookList
//         var dataUrl =''
//         var serverUrl
//         var accessToken
//         function GetUserConfig(data) {
//             console.log(data)
//             var data = JSON.parse(data)
//             if(data.userId){
//                 userId = data.userId
//             }
//             if(data.dataUrl){
//                 dataUrl = data.dataUrl
//             }
//             if(data.serverUrl){
//                 serverUrl = data.serverUrl
//             }
//             if(data.accessKey){
//                 accessKey = data.accessKey
//             }
//             if(data.accessToken){
//                 accessToken = data.accessToken 
//             }
//     }

userId = JSON.parse(sessionStorage.getItem('userId'));
accessKey = JSON.parse(sessionStorage.getItem('accessKey'));


if(JSON.parse(sessionStorage.getItem('dataList'))){
    var dataList = JSON.parse(sessionStorage.getItem('dataList'));
    var score = dataList[0];
    var totalIntegrity = dataList[1];
    var totalFluency = dataList[2];
    var totalAccuracy = dataList[3];
    dataList = [];
    sessionStorage.setItem('dataList',JSON.stringify(dataList));
    updateDayScore(score,totalIntegrity,totalFluency,totalAccuracy);
}



function updateDayScore(score,totalIntegrity,totalFluency,totalAccuracy){ 
    // alert(score)   
    console.log('score='+score)
            var content;
            var totalScore; 
            var average;
            var id;   
            var curDate = new Date();
            curDate = curDate.getFullYear() + '-' + (curDate.getMonth() + 1) + '-' + curDate.getDate();
            var jsonName = curDate + '.json'
            console.log(userId)
            var filename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/"+jsonName
            // console.log(filename)
            var pracScore = score ? score : 0
            // 判断是否存在当天的json文件
            $.ajax({
                url: filename,
                async: false,
                success: function(res){
                    console.log(res)
                    if(res){
                    id = res.id + 1;
                    totalScore = res.total + parseInt(pracScore);
                    average = (totalScore / id).toFixed(2);
                    }
                    content = JSON.stringify({id:id,total:parseInt(totalScore),score:parseInt(pracScore),average:Math.round(average)})

                },
                error: function(res){
                    console.log(res);
                    content = JSON.stringify({id:1,total:parseInt(pracScore),score:parseInt(pracScore),average:parseInt(pracScore)})
                }
            })
            //content = JSON.stringify({id:1,total:80,score:80,average:80})
            console.log(content)
            // 如果不加延时，上传会不生效
            setTimeout(function(){
                // alert("上传文件")
                writeFile(jsonName,content,'application/json');
                document.getElementById('postfiles').click();
                updateAnalysis(totalIntegrity,totalFluency,totalAccuracy);
            },100) 
        }
        
        // 更新测评分析
        // 测评分析保存的数据结构：

        function updateAnalysis(totalIntegrity,totalFluency,totalAccuracy){

            // 判断是否存在测评分析的json文件
            var aFilename ="https://szgs-100.oss-cn-beijing.aliyuncs.com/tools/"+userId+"/"+accessKey+"/eval_tools/analysis.json";
            var aRes = {}
            var totalAllIntegrity,totalAllFluency,totalAllAccuracy,id,averageIntegrity,averageFluency,averageAccuracy,IntegrityMax,FluencyMax,AccuracyMax,IntegrityMin,FluencyMin,AccuracyMin;  
            $.ajax({
                url: aFilename,
                async: false,
                success: function(res){
                    console.log(res)
                    // if(JSON.stringify(res) != '{}'){

                    if(res){
                        totalAllIntegrity = parseInt(res.totalAllIntegrity) + parseInt(totalIntegrity);
                        totalAllFluency = parseInt(res.totalAllFluency) + parseInt(totalFluency);
                        totalAllAccuracy = parseInt(res.totalAllAccuracy) + parseInt(totalAccuracy);
                        id = res.id +1;
                        averageIntegrity = (totalAllIntegrity / id).toFixed(2);
                        averageFluency = (totalAllFluency / id).toFixed(2);
                        averageAccuracy = (totalAllAccuracy / id).toFixed(2);
                        IntegrityMax = parseInt(res.IntegrityMax) < parseInt(totalIntegrity) ? parseInt(totalIntegrity) : parseInt(res.IntegrityMax);
                        FluencyMax = parseInt(res.FluencyMax) < parseInt(totalFluency) ? parseInt(totalFluency) : parseInt(res.FluencyMax);
                        AccuracyMax = parseInt(res.AccuracyMax) < parseInt(totalAccuracy) ? parseInt(totalAccuracy) : parseInt(res.AccuracyMax);
                        IntegrityMin = parseInt(res.IntegrityMin) > parseInt(totalIntegrity) ? parseInt(totalIntegrity) : parseInt(res.IntegrityMin);
                        FluencyMin = parseInt(res.FluencyMin) > parseInt(totalFluency) ? parseInt(totalFluency) : parseInt(res.FluencyMin);
                        AccuracyMin = parseInt(res.AccuracyMin) > parseInt(totalAccuracy) ? parseInt(totalAccuracy) : parseInt(res.AccuracyMin);
                    }

                    aRes = {
                            id:parseInt(id),
                            totalAllIntegrity:parseInt(totalAllIntegrity),
                            totalAllFluency:parseInt(totalAllFluency),
                            totalAllAccuracy:parseInt(totalAllAccuracy),
                            averageIntegrity:Math.round(averageIntegrity),
                            averageFluency:Math.round(averageFluency),
                            averageAccuracy:Math.round(averageAccuracy),
                            IntegrityMax:parseInt(IntegrityMax),
                            FluencyMax:parseInt(FluencyMax),
                            AccuracyMax:parseInt(AccuracyMax),
                            IntegrityMin:parseInt(IntegrityMin),
                            FluencyMin:parseInt(FluencyMin),
                            AccuracyMin:parseInt(AccuracyMin)       
                    }

                },
                error: function(res){
                    console.log(res)
                    aRes = {
                            id:1,
                            totalAllIntegrity:parseInt(totalIntegrity),
                            totalAllFluency:parseInt(totalFluency),
                            totalAllAccuracy:parseInt(totalAccuracy),
                            averageIntegrity:parseInt(totalIntegrity),
                            averageFluency:parseInt(totalFluency),
                            averageAccuracy:parseInt(totalAccuracy),
                            IntegrityMax:parseInt(totalIntegrity),
                            FluencyMax:parseInt(totalFluency),
                            AccuracyMax:parseInt(totalAccuracy),
                            IntegrityMin:parseInt(totalIntegrity),
                            FluencyMin:parseInt(totalFluency),
                            AccuracyMin:parseInt(totalAccuracy)
                    }
                }
            })

            var content = JSON.stringify(aRes);  // 存储到服务器的json数据
            console.log(content);
            // 如果不加延时，上传会不生效
            setTimeout(function(){
                writeFile('analysis.json',content,'application/json');
                document.getElementById('postfiles').click();
            },100) 
        }