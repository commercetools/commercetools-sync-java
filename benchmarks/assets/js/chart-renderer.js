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



    $.getJSON("https://commercetools.github.io/commercetools-sync-java/benchmarks/benchmarks.json", addData);

    function addData(data) {
        var count = 0;
        var commitHashArray = []
        var dataMap = []
        $.each(data, function (key, val) {
            commitHashArray.push(key);
            dataMap[key] = val
        })
        commitHashArray = commitHashArray.sort(function(hash1, hash2) {
            return hash1 - hash2
        })

        commitHashArray.forEach(function(commitHash) {
            var val = dataMap[commitHash];
            barChartData.labels.push(commitHash);

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
        })

//        $.each(data, function (key, val) {
//
//            barChartData.labels.push(key);
//
//            productSyncCreatesOnly.data.push(val.productSync.createsOnly.executionTime / 1000)
//            productSyncUpdatesOnly.data.push(val.productSync.updatesOnly.executionTime / 1000)
//            productSyncCreatesUpdates.data.push(val.productSync.mix.executionTime / 1000)
//
//            inventorySyncCreatesOnly.data.push(val.inventorySync.createsOnly.executionTime / 1000)
//
//            productTypeSyncCreatesOnly.data.push(val.productTypeSync.createsOnly.executionTime / 1000)
//            productTypeSyncUpdatesOnly.data.push(val.productTypeSync.updatesOnly.executionTime / 1000)
//            productTypeSyncCreatesUpdates.data.push(val.productTypeSync.mix.executionTime / 1000)
//
//            typeSyncCreatesOnly.data.push(val.typeSync.createsOnly.executionTime / 1000)
//            typeSyncUpdatesOnly.data.push(val.typeSync.updatesOnly.executionTime / 1000)
//            typeSyncCreatesUpdates.data.push(val.typeSync.mix.executionTime / 1000)
//
//            cartDiscountSyncCreatesOnly.data.push(val.cartDiscountSync.createsOnly.executionTime / 1000)
//            cartDiscountSyncUpdatesOnly.data.push(val.cartDiscountSync.updatesOnly.executionTime / 1000)
//            cartDiscountSyncCreatesUpdates.data.push(val.cartDiscountSync.mix.executionTime / 1000)
//
//        });
        window.myBar.update();
    }


};
