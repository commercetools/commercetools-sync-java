'use strict';

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

// red
var categorySyncCreatesOnly = {
    label: 'Category Sync (creates only)',
    backgroundColor: color(window.chartColors.red).alpha(0.2).rgbString(),
    borderColor: color(window.chartColors.red).alpha(0.2).rgbString(),
    borderWidth: 1,
    data: []
}
var categorySyncUpdatesOnly = {
    label: 'Category Sync (updates only)',
    backgroundColor: color(window.chartColors.red).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.red).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var categorySyncCreatesUpdates = {
    label: 'Category Sync (creates and updates)',
    backgroundColor: color(window.chartColors.red).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.red).alpha(0.6).rgbString(),
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
var inventorySyncUpdatesOnly = {
    label: 'Inventory Sync (updates only)',
    backgroundColor: color(window.chartColors.blue).alpha(0.4).rgbString(),
    borderColor: color(window.chartColors.blue).alpha(0.4).rgbString(),
    borderWidth: 1,
    data: []
}
var inventorySyncCreatesUpdates = {
    label: 'Inventory Sync (creates and updates)',
    backgroundColor: color(window.chartColors.blue).alpha(0.6).rgbString(),
    borderColor: color(window.chartColors.blue).alpha(0.6).rgbString(),
    borderWidth: 1,
    data: [],
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




var barChartData = {
    labels: [],
    datasets: [
        productSyncCreatesOnly,
        productSyncUpdatesOnly,
        productSyncCreatesUpdates,

        categorySyncCreatesOnly,
        categorySyncUpdatesOnly,
        categorySyncCreatesUpdates,

        inventorySyncCreatesOnly,
        inventorySyncUpdatesOnly,
        inventorySyncCreatesUpdates,

        productTypeSyncCreatesOnly,
        productTypeSyncUpdatesOnly,
        productTypeSyncCreatesUpdates,

        typeSyncCreatesOnly,
        typeSyncUpdatesOnly,
        typeSyncCreatesUpdates
    ]

};

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
                        labelString: 'Time to sync 10000 resources (in seconds)',
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



    $.getJSON("https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json", addData);

    function addData(data) {
        $.each(data, function (key, val) {
            barChartData.labels.push(key);

            productSyncCreatesOnly.data.push(val.productSync.createsOnly.average / 1000)
            productSyncUpdatesOnly.data.push(val.productSync.updatesOnly.average / 1000)
            productSyncCreatesUpdates.data.push(val.productSync.mix.average / 1000)

            categorySyncCreatesOnly.data.push(val.categorySync.createsOnly.average / 1000)
            categorySyncUpdatesOnly.data.push(val.categorySync.updatesOnly.average / 1000)
            categorySyncCreatesUpdates.data.push(val.categorySync.mix.average / 1000)

            inventorySyncCreatesOnly.data.push(val.inventorySync.createsOnly.average / 1000)
            inventorySyncUpdatesOnly.data.push(val.inventorySync.updatesOnly.average / 1000)
            inventorySyncCreatesUpdates.data.push(val.inventorySync.mix.average / 1000)


            if (val.productTypeSync) {
                //console.log("product type Sync k:" + key)
                //console.log("product type Sync v:" + JSON.stringify(val))
                productTypeSyncCreatesOnly.data.push(val.productTypeSync.createsOnly.average / 1000)
                productTypeSyncUpdatesOnly.data.push(val.productTypeSync.updatesOnly.average / 1000)
                productTypeSyncCreatesUpdates.data.push(val.productTypeSync.mix.average / 1000)
            } else {
                productTypeSyncCreatesOnly.data.push(0)
                productTypeSyncUpdatesOnly.data.push(0)
                productTypeSyncCreatesUpdates.data.push(0)
            }

            if (val.typeSync) {
                typeSyncCreatesOnly.data.push(val.typeSync.createsOnly.average / 1000)
                typeSyncUpdatesOnly.data.push(val.typeSync.updatesOnly.average / 1000)
                typeSyncCreatesUpdates.data.push(val.typeSync.mix.average / 1000)
            } else {
                typeSyncCreatesOnly.data.push(0)
                typeSyncUpdatesOnly.data.push(0)
                typeSyncCreatesUpdates.data.push(0)
            }
        });
        console.log(barChartData);
        window.myBar.update();
    }


};