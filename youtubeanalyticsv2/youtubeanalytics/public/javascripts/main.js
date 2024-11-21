// Documentation: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

// // Create WebSocket connection.
// const socket = new WebSocket("ws://localhost:9000/ws");

// // Connection opened
// socket.addEventListener("open", (event) => {
//     document.getElementById("status").innerHTML = "Connected";
// });

// socket.addEventListener("close", (event) => {
//     document.getElementById("status").innerHTML = "Disconnected";
// });

// // Listen for messages
// socket.addEventListener("message", (event) => {
//     var timeStr = document.createElement("li");
//     timeStr.textContent = event.data;
//     document.getElementById("time-tick").appendChild(timeStr);
// });

const socket = new WebSocket(`ws://${window.location.host}/ws`);

socket.onopen = () => {
	console.log("WebSocket connection established");
};

socket.onmessage = (event) => {
	const data = JSON.parse(event.data);

	console.log(data);
	switch (data.status) {
		case "started":
			showStatus("Analysis started...");
			break;

		case "searching":
			break;

		case "completed":
			console.log(data);
			showVideos(data.data.items);
			showSentimentResult(data.data);
			break;
	}
};

function startAnalysis(query) {
	socket.send(
		JSON.stringify({
			action: "search",
			query: query,
		}),
	);
}

function showStatus(message) {
	document.getElementById("status").textContent = message;
}

function showVideos(videos) {
	const videoList = document.getElementById("videoList");
	videoList.innerHTML = videos
		.map(
			(video) => `
        <div class="video-item">
            <img src="${video.thumbnailUrl}" alt="${video.title}">
            <h3>${video.title}</h3>
            <p>${video.description}</p>
        </div>
    `,
		)
		.join("");
}

function showSentimentResult(result) {
	document.getElementById("sentiment").textContent =
		`Sentiment: ${result.sentiment} (Score: ${result.score})`;
}
