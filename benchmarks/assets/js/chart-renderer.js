'use strict';

window.chartColors = {
    red: 'rgb(255, 99, 132)',
    orange: 'rgb(255, 159, 64)',
    yellow: 'rgb(255, 205, 86)',
    green: 'rgb(75, 192, 192)',
    blue: 'rgb(54, 162, 235)',
    purple: 'rgb(153, 102, 255)',
    grey: 'rgb(201, 203, 207)'
};


var color = Chart.helpers.color;
var productSyncCreatesOnly = {
    label: 'Product Sync (creates only)',
    backgroundColor: color(window.chartColors.yellow).alpha(0.5).rgbString(),
    borderColor: window.chartColors.yellow,
    borderWidth: 1,
    data: []
}
var productSyncUpdatesOnly = {
    label: 'Product Sync (updates only)',
    backgroundColor: color(window.chartColors.green).alpha(0.5).rgbString(),
    borderColor: window.chartColors.green,
    borderWidth: 1,
    data: []
}
var productSyncCreatesUpdates = {
    label: 'Product Sync (creates and updates)',
    backgroundColor: color(window.chartColors.orange).alpha(0.5).rgbString(),
    borderColor: window.chartColors.orange,
    borderWidth: 1,
    data: []
}
var categorySyncCreatesOnly = {
    label: 'Category Sync (creates only)',
    backgroundColor: color(window.chartColors.red).alpha(0.5).rgbString(),
    borderColor: window.chartColors.red,
    borderWidth: 1,
    data: []
}
var categorySyncUpdatesOnly = {
    label: 'Category Sync (updates only)',
    backgroundColor: color(window.chartColors.blue).alpha(0.5).rgbString(),
    borderColor: window.chartColors.blue,
    borderWidth: 1,
    data: []
}
var categorySyncCreatesUpdates = {
    label: 'Category Sync (creates and updates)',
    backgroundColor: color(window.chartColors.grey).alpha(0.5).rgbString(),
    borderColor: window.chartColors.grey,
    borderWidth: 1,
    data: []
}
var inventorySyncCreatesOnly = {
    label: 'Inventory Sync (creates only)',
    backgroundColor: color(window.chartColors.purple).alpha(0.5).rgbString(),
    borderColor: window.chartColors.purple,
    borderWidth: 1,
    data: []
}
var inventorySyncUpdatesOnly = {
    label: 'Inventory Sync (updates only)',
    backgroundColor: color(window.chartColors.orange).alpha(0.9).rgbString(),
    borderColor: window.chartColors.orange,
    borderWidth: 1,
    data: []
}
var inventorySyncCreatesUpdates = {
    label: 'Inventory Sync (creates and updates)',
    backgroundColor: color(window.chartColors.red).alpha(0.9).rgbString(),
    borderColor: window.chartColors.red,
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
        inventorySyncCreatesUpdates
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
                    fontColor: 'white'
                }
            },
            title: {
                display: true,
                text: 'commercetools-sync-java Benchmarks',
                fontColor: 'white',
                fontSize: 20
            },
            scales: {
                yAxes: [{
                    ticks : {
                        fontColor: 'white'
                    },
                    scaleLabel: {
                        display: true,
                        labelString: 'Time to sync 10000 resources (in seconds)',
                        fontColor: 'white'
                    }
                }],
                xAxes: [{
                    ticks : {
                        fontColor: 'white'
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
        });
        window.myBar.update();
    }


};