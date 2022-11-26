function search(){
    let query=$("#search-input").val();
    if(query==""){
        $(".search-result").hide();
    }
    else{
        let url=`http://localhost:8080/search/${query}`;
        fetch(url).then((response)=>{
            return response.json();
        }).then((data)=>{
            let list=`<div class="list-group">`;
            data.forEach(contact => {
                list+=`<a href='/user/contact/${contact.cid}' class="list-group-item list-group-item-action">${contact.name}</a>`;
            });
            list+=`</div>`;
            $(".search-result").html(list);
            $(".search-result").show();
        });
    }
};

function paymentStart(){
    let amount=$("#payment_field").val();
    if(amount=="" || amount==null){
        swal("Sry !", "Amount is required !", "error");
        return;
    }
    $.ajax(
        {
            url: '/create_order',
            data: JSON.stringify({amount:amount, info:"order_request"}),
            contentType:"application/json",
            type:"POST",
            dataType:"json",
            success:function(response){
                if(response.status=="created"){
                    let options = {
                        key: "rzp_live_bxZxQiMNkAl4q2",
                        amount: response.amount,
                        currency: "INR",
                        name: "Smart Contact Manager",
                        description: "Donation",
                        order_id: response.id,
                        handler: function (response){
                            swal("Congrats !", "Payment Sucessful !", "success");
                            document.getElementById("payment_field").value="";
                        },
                        prefill: {
                        name: "Smart Contact Manager Private Ltd.",
                        email: "bspadhy1999@gmail.com",
                        contact: "6370023010"
                        },
                        notes: {
                        address: "Smart Contact Manager Private Ltd."
                        },
                        };
                        let rzp = new Razorpay(options);
                        rzp.on("payment.failed", function (response){
                            swal("Sry !", "Oops payment failed !", "error");
                        });
                        rzp.open();
                }
            },error:function(error){
                alert("Something went wrong !");
            },
        }
    );
};

