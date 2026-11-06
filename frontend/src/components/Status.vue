<template><span class="status" :class="tone">{{ text }}</span></template>
<script>
import {label} from '../api';
export default {props:{value:String,execution:Boolean},computed:{text(){return this.execution&&this.value==='REGISTERED'?'已登记':label(this.value);},tone(){return ['PAID','COMPLETED','DIAGNOSED','DISPENSED'].includes(this.value)?'green':['CANCELLED','REFUNDED','RETURNED'].includes(this.value)?'gray':['REGISTERED','UNPAID','PENDING'].includes(this.value)?'amber':'blue';}}};
</script>
