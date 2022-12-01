'use strict';

var numberOfDisplayedCommits = 10
var versionNumberArray = []
var barChartDataMap = []

window.chartColors = {
    red: 'rgb(255, 99, 132)',
    orange: 'rgb(242, 121, 35)',
    yellow: 'rgb(245, 167, 48)',
    green: 'rgb(32, 173, 146)',
    blue: 'rgb(42, 65, 72)',
    skyBlue: 'rgb(13, 157, 248)',
    purple: 'rgb(153, 102, 255)',
    lightGrey: 'rgb(208, 209, 209)',
    mediumGrey: 'rgb(129, 129, 133)'
};


var color = Chart.helpers.color;
// yellow
var productSyncCreatesOnly = {
    label: 'Product Sync (creates only)',
    backgroundColor: color(window.chartColors.yellow).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.yellow).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}
var productSyncUpdatesOnly = {
    label: 'Product Sync (updates only)',
    backgroundColor: color(window.chartColors.yellow).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.yellow).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var productSyncCreatesUpdates = {
    label: 'Product Sync (creates and updates)',
    backgroundColor: color(window.chartColors.yellow).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.yellow).alpha(0.6).rgbString(),
    borderWidth: 1,
    data: []
}

// blue
var inventorySyncCreatesOnly = {
    label: 'Inventory Sync (creates only)',
    backgroundColor: color(window.chartColors.blue).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.blue).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}

// skyBlue
var productTypeSyncCreatesOnly = {
    label: 'ProductType Sync (creates only)',
    backgroundColor: color(window.chartColors.skyBlue).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.skyBlue).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}
var productTypeSyncUpdatesOnly = {
    label: 'ProductType Sync (updates only)',
    backgroundColor: color(window.chartColors.skyBlue).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.skyBlue).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var productTypeSyncCreatesUpdates = {
    label: 'ProductType Sync (creates and updates)',
    backgroundColor: color(window.chartColors.skyBlue).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.skyBlue).alpha(0.6).rgbString(),
    borderWidth: 1,
    data: [],
}

// green
var typeSyncCreatesOnly = {
    label: 'Type Sync (creates only)',
    backgroundColor: color(window.chartColors.green).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.green).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}
var typeSyncUpdatesOnly = {
    label: 'Type Sync (updates only)',
    backgroundColor: color(window.chartColors.green).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.green).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var typeSyncCreatesUpdates = {
    label: 'Type Sync (creates and updates)',
    backgroundColor: color(window.chartColors.green).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.green).alpha(0.6).rgbString(),
    borderWidth: 1,
    data: [],
}

// purple
var cartDiscountSyncCreatesOnly = {
    label: 'CartDiscount Sync (creates only)',
    backgroundColor: color(window.chartColors.purple).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.purple).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}
var cartDiscountSyncUpdatesOnly = {
    label: 'CartDiscount Sync (updates only)',
    backgroundColor: color(window.chartColors.purple).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.purple).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var cartDiscountSyncCreatesUpdates = {
    label: 'CartDiscount Sync (creates and updates)',
    backgroundColor: color(window.chartColors.purple).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.purple).alpha(0.6).rgbString(),
    borderWidth: 1,
    data: [],
}

var barChartData = {
    labels: [],
    datasets: [
        productSyncCreatesOnly,
        productSyncUpdatesOnly,
        productSyncCreatesUpdates,

        inventorySyncCreatesOnly,

        productTypeSyncCreatesOnly,
        productTypeSyncUpdatesOnly,
        productTypeSyncCreatesUpdates,

        typeSyncCreatesOnly,
        typeSyncUpdatesOnly,
        typeSyncCreatesUpdates,

        cartDiscountSyncCreatesOnly,
        cartDiscountSyncUpdatesOnly,
        cartDiscountSyncCreatesUpdates
    ]
};

function clearBarData() {
   barChartData.labels=[]
   productSyncCreatesOnly.data = []
   productSyncUpdatesOnly.data = []
   productSyncCreatesUpdates.data = []
   inventorySyncCreatesOnly.data = []
   productTypeSyncCreatesOnly.data = []
   productTypeSyncUpdatesOnly.data = []
   productTypeSyncUpdatesOnly.data = []
   productTypeSyncCreatesUpdates.data = []
   typeSyncCreatesOnly.data = []
   typeSyncUpdatesOnly.data = []
   typeSyncCreatesUpdates.data = []
   cartDiscountSyncCreatesOnly.data = []
   cartDiscountSyncUpdatesOnly.data = []
   cartDiscountSyncCreatesUpdates.data = []
}

function compareBarChartData() {
    clearBarData();
    var versionNumber = [];
    for (var i=1; i<=2; i++) {
        versionNumber[i-1] = document.getElementById("versionTagDropDownBox"+i).value;
        if (versionNumber[i-1]!="") {
            var val = barChartDataMap[versionNumber[i-1]];
            barChartData.labels.push(versionNumber[i-1]);
            productSyncCreatesOnly.data.push(val.productSync.createsOnly.executionTime / 1000)
            productSyncUpdatesOnly.data.push(val.productSync.updatesOnly.executionTime / 1000)
            productSyncCreatesUpdates.data.push(val.productSync.mix.executionTime / 1000)
            inventorySyncCreatesOnly.data.push(val.inventorySync.createsOnly.executionTime / 1000)
            productTypeSyncCreatesOnly.data.push(val.productTypeSync.createsOnly.executionTime / 1000)
            productTypeSyncUpdatesOnly.data.push(val.productTypeSync.updatesOnly.executionTime / 1000)
            productTypeSyncCreatesUpdates.data.push(val.productTypeSync.mix.executionTime / 1000)
            typeSyncCreatesOnly.data.push(val.typeSync.createsOnly.executionTime / 1000)
            typeSyncUpdatesOnly.data.push(val.typeSync.updatesOnly.executionTime / 1000)
            typeSyncCreatesUpdates.data.push(val.typeSync.mix.executionTime / 1000)
            cartDiscountSyncCreatesOnly.data.push(val.cartDiscountSync.createsOnly.executionTime / 1000)
            cartDiscountSyncUpdatesOnly.data.push(val.cartDiscountSync.updatesOnly.executionTime / 1000)
            cartDiscountSyncCreatesUpdates.data.push(val.cartDiscountSync.mix.executionTime / 1000)
        }
    }
    window.myBar.update();
}

function initDropDownBoxItem(dropDownBoxItems) {

    var dropDownBox1 = document.getElementById("versionTagDropDownBox1");
    var dropDownBox2 = document.getElementById("versionTagDropDownBox2");
    for(var i = dropDownBoxItems.length-1; i >= 0; --i) {
        var option1 = document.createElement('option');
        var option2 = document.createElement('option');
        option1.text = option1.value = dropDownBoxItems[i];
        option2.text = option2.value = dropDownBoxItems[i];
        dropDownBox1.add(option1);
        dropDownBox2.add(option2);
    }
    dropDownBox1.options[0].selected = true
    dropDownBox2.options[0].selected = true
}

function prepareBarChartDataCache(data) {
    $.each(data, function (key, val) {
        versionNumberArray.push(key);
        barChartDataMap[key] = val
    })
    versionNumberArray = versionNumberArray.reverse()
    initDropDownBoxItem(versionNumberArray)
}
function addDataToChart(data) {
    var count = 0
    console.log("versionNumberArray length : " + versionNumberArray.length)
    if(versionNumberArray.length==0) {
        prepareBarChartDataCache(data)
    }

    versionNumberArray.forEach(function(versionNumber) {
        if (count < numberOfDisplayedCommits) {
            var val = barChartDataMap[versionNumber];
            barChartData.labels.push(versionNumber);

            productSyncCreatesOnly.data.push(val.productSync.createsOnly.executionTime / 1000)
            productSyncUpdatesOnly.data.push(val.productSync.updatesOnly.executionTime / 1000)
            productSyncCreatesUpdates.data.push(val.productSync.mix.executionTime / 1000)

            inventorySyncCreatesOnly.data.push(val.inventorySync.createsOnly.executionTime / 1000)

            productTypeSyncCreatesOnly.data.push(val.productTypeSync.createsOnly.executionTime / 1000)
            productTypeSyncUpdatesOnly.data.push(val.productTypeSync.updatesOnly.executionTime / 1000)
            productTypeSyncCreatesUpdates.data.push(val.productTypeSync.mix.executionTime / 1000)

            typeSyncCreatesOnly.data.push(val.typeSync.createsOnly.executionTime / 1000)
            typeSyncUpdatesOnly.data.push(val.typeSync.updatesOnly.executionTime / 1000)
            typeSyncCreatesUpdates.data.push(val.typeSync.mix.executionTime / 1000)

            cartDiscountSyncCreatesOnly.data.push(val.cartDiscountSync.createsOnly.executionTime / 1000)
            cartDiscountSyncUpdatesOnly.data.push(val.cartDiscountSync.updatesOnly.executionTime / 1000)
            cartDiscountSyncCreatesUpdates.data.push(val.cartDiscountSync.mix.executionTime / 1000)
        }
        count++
    })
    window.myBar.update();
}

function showDataForLatestVersions() {
    clearBarData()
    $.getJSON("https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json", addDataToChart);
}

window.onload = function () {
    var ctx = document.getElementById("canvas").getContext("2d");
    window.myBar = new Chart(ctx, {
        type: 'bar',
        data: barChartData,
        options: {
            responsive: true,
            legend: {
                labels: {
                    fontColor: 'black'
                }
            },
            title: {
                display: true,
                text: 'commercetools-sync-java Benchmarks',
                fontColor: 'black',
                fontSize: 20
            },
            scales: {
                yAxes: [{
                    ticks : {
                        fontColor: 'black'
                    },
                    scaleLabel: {
                        display: true,
                        labelString: 'Time to sync 1000 resources (in seconds)',
                        fontColor: 'black'
                    }
                }],
                xAxes: [{
                    ticks : {
                        fontColor: 'black'
                    }
                }]
            }
        }
    });

    showDataForLatestVersions()
};
