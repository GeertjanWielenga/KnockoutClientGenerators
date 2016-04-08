define(['ojs/ojcore', 'knockout', 'ojs/ojtable', 'ojs/ojdatacollection-common'
], function (oj, ko) {
    function discountContentViewModel() {
        var self = this;
        self.data = ko.observableArray();
        $.getJSON("http://localhost:8080/CustomerBackend/webresources/com.mycompany.customerbackend.micromarket/").
                then(function (micromarkets) {
                    $.each(micromarkets, function () {
                        self.data.push({
                            areaLength: this.areaLength,
                            areaWidth: this.areaWidth,
                            radius: this.radius,
                        });
                    });
                });
        self.datasource = new oj.ArrayTableDataSource(
                self.data,
                {idAttribute: 'areaLength'}
        );
//            var deptArray = [
//                {DepartmentId: 20, DepartmentName: 'History'},
//                {DepartmentId: 10, DepartmentName: 'Geography'},
//                {DepartmentId: 30, DepartmentName: 'Biology'}];
//            self.datasource =
//                    new oj.ArrayTableDataSource(
//                            deptArray,
//                            {idAttribute: 'DepartmentId'});
    }
    return discountContentViewModel;
});