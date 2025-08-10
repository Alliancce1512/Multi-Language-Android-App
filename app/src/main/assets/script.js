(function () {
  const button = document.getElementById('helloBtn');
  const message = document.getElementById('helloMsg');
  if (!button || !message) return;

  button.addEventListener('click', () => {
    const now = new Date().toLocaleString();
    message.textContent = `Hello from local JavaScript! It is ${now}.`;
  });
})();