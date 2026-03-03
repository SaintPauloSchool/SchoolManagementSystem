<template>
  <div id="app">
    <SchoolNotificationSystem />
  </div>
</template>

<script>
import SchoolNotificationSystem from './components/SchoolNotificationSystem.vue'

export default {
  name: 'App',
  components: {
    SchoolNotificationSystem
  }
}
</script>

<style>
@import './styles/modern-theme.css';

#app {
  height: 100vh;
  width: 100%;
  overflow: hidden;
}
</style>